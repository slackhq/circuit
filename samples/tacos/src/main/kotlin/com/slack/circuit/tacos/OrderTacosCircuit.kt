// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.step.ConfirmationOrderStep
import com.slack.circuit.tacos.step.ConfirmationProducer
import com.slack.circuit.tacos.step.ConfirmationUi
import com.slack.circuit.tacos.step.FillingsOrderStep
import com.slack.circuit.tacos.step.FillingsProducer
import com.slack.circuit.tacos.step.FillingsUi
import com.slack.circuit.tacos.step.OrderStep
import com.slack.circuit.tacos.step.SummaryOrderStep
import com.slack.circuit.tacos.step.SummaryProducer
import com.slack.circuit.tacos.step.SummaryUi
import com.slack.circuit.tacos.step.ToppingsOrderStep
import com.slack.circuit.tacos.step.ToppingsProducer
import com.slack.circuit.tacos.step.ToppingsUi
import java.math.BigDecimal
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.Parcelize

@Parcelize
object OrderTacosScreen : Screen {
  data class State(
    @StringRes val headerResId: Int,
    val orderCost: BigDecimal,
    val stepState: OrderStep.State,
    val isPreviousVisible: Boolean,
    val isNextEnabled: Boolean,
    val isNextVisible: Boolean,
    val isFooterVisible: Boolean,
    val eventSink: (Event) -> Unit
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    val indexModifier: Int
      get() = 0

    object Previous : Event {
      override val indexModifier: Int = -1
    }
    object Next : Event {
      override val indexModifier: Int = 1
    }
    object ProcessOrder : Event {
      override val indexModifier: Int = 1
    }
  }
}

@Immutable
data class OrderDetails(
  val filling: Ingredient = Ingredient(""),
  val toppings: Set<Ingredient> = persistentSetOf(),
  val baseCost: BigDecimal = BigDecimal("9.99"), // Default taco price
  val ingredientsCost: BigDecimal = BigDecimal.ZERO
)

private val orderSteps =
  persistentListOf(FillingsOrderStep, ToppingsOrderStep, ConfirmationOrderStep, SummaryOrderStep)

internal class OrderTacosPresenter(
  private val fillingsProducer: FillingsProducer,
  private val toppingsProducer: ToppingsProducer,
  private val confirmationProducer: ConfirmationProducer,
  private val summaryProducer: SummaryProducer,
) : Presenter<OrderTacosScreen.State> {
  @Composable override fun present(): OrderTacosScreen.State = presentInternal()

  @Composable
  internal fun presentInternal(
    initialStep: OrderStep = FillingsOrderStep,
    initialOrderDetails: OrderDetails = OrderDetails(),
  ): OrderTacosScreen.State {
    var currentStep by remember { mutableStateOf(initialStep) }
    var orderDetails by remember { mutableStateOf(initialOrderDetails) }
    var isNextEnabled by remember { mutableStateOf(false) }

    val stepState =
      produceState(currentStep, orderDetails) { event ->
        when (event) {
          is OrderStep.Validation -> isNextEnabled = event.isOrderValid
          is OrderStep.UpdateOrder -> orderDetails = updateOrder(event, orderDetails)
          is OrderStep.Restart -> {
            currentStep = FillingsOrderStep
            orderDetails = OrderDetails()
            isNextEnabled = false
          }
        }
      }

    return OrderTacosScreen.State(
      headerResId = currentStep.headerResId,
      orderCost = orderDetails.baseCost + orderDetails.ingredientsCost,
      stepState = stepState,
      isPreviousVisible = currentStep.index in 1..2,
      isNextEnabled = isNextEnabled,
      isNextVisible = currentStep.index < 2,
      isFooterVisible = currentStep != SummaryOrderStep
    ) { navEvent ->
      processNavigation(currentStep, navEvent) { currentStep = it }
    }
  }

  @Composable
  private fun produceState(
    orderStep: OrderStep,
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ): OrderStep.State =
    when (orderStep) {
      is FillingsOrderStep -> fillingsProducer(orderDetails, eventSink)
      is ToppingsOrderStep -> toppingsProducer(orderDetails, eventSink)
      is ConfirmationOrderStep -> confirmationProducer(orderDetails, eventSink)
      is SummaryOrderStep -> summaryProducer(orderDetails, eventSink)
    }
}

private fun processNavigation(
  currentStep: OrderStep,
  navEvent: OrderTacosScreen.Event,
  onNavEvent: (OrderStep) -> Unit
) {
  val newIndex = currentStep.index + navEvent.indexModifier
  onNavEvent(orderSteps.getOrElse(newIndex) { currentStep })
}

private fun updateOrder(
  event: OrderStep.UpdateOrder,
  currentOrder: OrderDetails,
): OrderDetails {
  val newFilling =
    when (event) {
      is OrderStep.UpdateOrder.SetFilling -> event.ingredient
      else -> currentOrder.filling
    }
  val newToppings =
    when (event) {
      is OrderStep.UpdateOrder.SetToppings -> event.ingredients
      else -> currentOrder.toppings
    }

  val newIngredientsCost = calculateIngredientsCost(newFilling, newToppings)

  return OrderDetails(
    baseCost = currentOrder.baseCost,
    ingredientsCost = newIngredientsCost,
    filling = newFilling,
    toppings = newToppings.toImmutableSet()
  )
}

private fun calculateIngredientsCost(filling: Ingredient?, toppings: Set<Ingredient>): BigDecimal {
  var cost = filling?.charge ?: BigDecimal.ZERO
  toppings.forEach { topping -> cost += topping.charge }
  return cost
}

@Composable
internal fun OrderTacosUi(state: OrderTacosScreen.State, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  Scaffold(
    modifier = modifier.padding(5.dp),
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(state.headerResId)) },
        modifier = Modifier,
        navigationIcon = {
          NavigationButton(
            direction = Direction.LEFT,
            visible = state.isPreviousVisible,
          ) {
            sink(OrderTacosScreen.Event.Previous)
          }
        },
        actions = {
          NavigationButton(
            direction = Direction.RIGHT,
            enabled = state.isNextEnabled,
            visible = state.isNextVisible,
          ) {
            sink(OrderTacosScreen.Event.Next)
          }
        }
      )
    },
    bottomBar = {
      OrderTotal(
        orderCost = state.orderCost,
        onConfirmationStep = state.stepState is ConfirmationOrderStep.Order,
        isVisible = state.isFooterVisible,
      ) {
        sink(OrderTacosScreen.Event.ProcessOrder)
      }
    }
  ) { padding ->
    val stepModifier = Modifier.padding(padding)
    when (state.stepState) {
      is FillingsOrderStep.State -> FillingsUi(state.stepState, stepModifier)
      is ToppingsOrderStep.State -> ToppingsUi(state.stepState, stepModifier)
      is ConfirmationOrderStep.Order -> ConfirmationUi(state.stepState, stepModifier)
      is SummaryOrderStep.SummaryState -> SummaryUi(state.stepState, stepModifier)
    }
  }
}

internal enum class Direction(val icon: ImageVector, @StringRes val descriptionResId: Int) {
  LEFT(Icons.Filled.ArrowBack, R.string.top_bar_back),
  RIGHT(Icons.Filled.ArrowForward, R.string.top_bar_forward)
}

@Composable
private fun NavigationButton(
  direction: Direction,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  visible: Boolean = true,
  onClick: () -> Unit
) {
  if (!visible) return

  val tintColour =
    when (enabled) {
      true -> MaterialTheme.colorScheme.onSurface
      false -> MaterialTheme.colorScheme.outline
    }

  IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
    Image(
      modifier = Modifier,
      painter = rememberVectorPainter(image = direction.icon),
      colorFilter = ColorFilter.tint(tintColour),
      contentDescription = stringResource(direction.descriptionResId),
    )
  }
}

@Composable
private fun OrderTotal(
  orderCost: BigDecimal,
  onConfirmationStep: Boolean,
  isVisible: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  if (!isVisible) return

  val color =
    when {
      onConfirmationStep -> MaterialTheme.colorScheme.onTertiaryContainer
      else -> MaterialTheme.colorScheme.tertiaryContainer
    }
  var boxModifier =
    modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 30.dp)
      .padding(horizontal = 5.dp)
      .clip(RoundedCornerShape(5.dp))
      .background(color)
  if (onConfirmationStep) boxModifier = boxModifier.clickable(onClick = onClick)

  val label =
    when (onConfirmationStep) {
      true -> stringResource(R.string.bottom_bar_place_order)
      false -> stringResource(R.string.bottom_bar_order_total)
    }
  Box(modifier = boxModifier) {
    Text(
      text = label,
      modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp),
      color = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = if (onConfirmationStep) "$$orderCost" else "$$orderCost",
      modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
      color = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.Bold,
    )
  }
}

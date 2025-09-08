// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.tacos.model.Cents
import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.model.toCurrencyString
import com.slack.circuit.tacos.step.ConfirmationOrderStep
import com.slack.circuit.tacos.step.ConfirmationUi
import com.slack.circuit.tacos.step.FillingsOrderStep
import com.slack.circuit.tacos.step.FillingsUi
import com.slack.circuit.tacos.step.OrderStep
import com.slack.circuit.tacos.step.SummaryOrderStep
import com.slack.circuit.tacos.step.SummaryUi
import com.slack.circuit.tacos.step.ToppingsOrderStep
import com.slack.circuit.tacos.step.ToppingsUi
import com.slack.circuit.tacos.ui.theme.TacoTheme
import kotlinx.parcelize.Parcelize

@Parcelize
data object OrderTacosScreen : Screen {
  data class State(
    @StringRes val headerResId: Int,
    val orderCost: String,
    val stepState: OrderStep.State,
    val isPreviousVisible: Boolean,
    val isNextEnabled: Boolean,
    val isNextVisible: Boolean,
    val isFooterVisible: Boolean,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    val indexModifier: Int

    data object Previous : Event {
      override val indexModifier: Int = -1
    }

    data object Next : Event {
      override val indexModifier: Int = 1
    }

    data object ProcessOrder : Event {
      override val indexModifier: Int = 1
    }
  }
}

private const val TACO_BASE_PRICE = 999

@Immutable
data class OrderDetails(
  val filling: Ingredient = Ingredient(""),
  val toppings: Set<Ingredient> = emptySet(),
  val ingredientsCost: Cents = 0,
) {
  val baseCost: Cents = TACO_BASE_PRICE
}

/** List of wizard steps (in order!) */
private val orderSteps =
  listOf(FillingsOrderStep, ToppingsOrderStep, ConfirmationOrderStep, SummaryOrderStep)

internal class OrderTacosPresenter(
  private val fillingsProducer: OrderStep.StateProducer<FillingsOrderStep.State>,
  private val toppingsProducer: OrderStep.StateProducer<ToppingsOrderStep.State>,
  private val confirmationProducer: OrderStep.StateProducer<ConfirmationOrderStep.OrderState>,
  private val summaryProducer: OrderStep.StateProducer<SummaryOrderStep.SummaryState>,
  private val initialStep: OrderStep = FillingsOrderStep,
  private val initialOrderDetails: OrderDetails = OrderDetails(),
) : Presenter<OrderTacosScreen.State> {
  @Composable
  override fun present(): OrderTacosScreen.State {
    var currentStep by remember { mutableStateOf(initialStep) }
    var orderDetails by remember { mutableStateOf(initialOrderDetails) }
    var isNextEnabled by remember { mutableStateOf(false) }

    // Generate state for the current order step
    val stepState =
      produceOrderStepState(currentStep, orderDetails) { event ->
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
      orderCost = (orderDetails.baseCost + orderDetails.ingredientsCost).toCurrencyString(),
      stepState = stepState,
      isPreviousVisible = currentStep.index in 1..2,
      isNextEnabled = isNextEnabled,
      isNextVisible = currentStep.index < 2,
      isFooterVisible = currentStep != SummaryOrderStep,
    ) { navEvent ->
      // process navigation event from child order step
      processNavigation(currentStep, navEvent) { currentStep = it }
    }
  }

  @Composable
  private fun produceOrderStepState(
    orderStep: OrderStep,
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit,
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
  onNavEvent: (OrderStep) -> Unit,
) {
  val newIndex = currentStep.index + navEvent.indexModifier
  onNavEvent(orderSteps[newIndex])
}

private fun updateOrder(event: OrderStep.UpdateOrder, currentOrder: OrderDetails): OrderDetails {
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
    ingredientsCost = newIngredientsCost,
    filling = newFilling,
    toppings = newToppings,
  )
}

private fun calculateIngredientsCost(filling: Ingredient?, toppings: Set<Ingredient>): Cents {
  var cost = filling?.charge ?: 0
  toppings.forEach { topping -> cost += topping.charge }
  return cost
}

@Composable
internal fun OrderTacosUi(state: OrderTacosScreen.State, modifier: Modifier = Modifier) {
  BackHandler(enabled = state.isPreviousVisible) {
    state.eventSink(OrderTacosScreen.Event.Previous)
  }

  Scaffold(
    modifier = modifier.padding(4.dp),
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(state.headerResId)) },
        modifier = Modifier,
        navigationIcon = {
          NavigationButton(direction = Direction.LEFT, visible = state.isPreviousVisible) {
            state.eventSink(OrderTacosScreen.Event.Previous)
          }
        },
        actions = {
          NavigationButton(
            direction = Direction.RIGHT,
            enabled = state.isNextEnabled,
            visible = state.isNextVisible,
          ) {
            state.eventSink(OrderTacosScreen.Event.Next)
          }
        },
      )
    },
  ) { padding ->
    Column(Modifier.padding(padding).fillMaxSize()) {
      val stepModifier = Modifier.weight(1f)
      when (state.stepState) {
        is FillingsOrderStep.State -> FillingsUi(state.stepState, modifier = stepModifier)
        is ToppingsOrderStep.State -> ToppingsUi(state.stepState, modifier = stepModifier)
        is ConfirmationOrderStep.OrderState ->
          ConfirmationUi(state.stepState, modifier = stepModifier)
        is SummaryOrderStep.SummaryState -> SummaryUi(state.stepState, modifier = stepModifier)
      }
      OrderTotal(
        orderCost = state.orderCost,
        onConfirmationStep = state.stepState is ConfirmationOrderStep.OrderState,
        isVisible = state.isFooterVisible,
      ) {
        state.eventSink(OrderTacosScreen.Event.ProcessOrder)
      }
    }
  }
}

internal enum class Direction(val icon: ImageVector, @StringRes val descriptionResId: Int) {
  LEFT(Icons.AutoMirrored.Filled.ArrowBack, R.string.top_bar_back),
  RIGHT(Icons.AutoMirrored.Filled.ArrowForward, R.string.top_bar_forward),
}

@Composable
private fun NavigationButton(
  direction: Direction,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  visible: Boolean = true,
  onClick: () -> Unit,
) {
  if (!visible) return

  IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
    Icon(
      painter = rememberVectorPainter(image = direction.icon),
      contentDescription = stringResource(direction.descriptionResId),
    )
  }
}

@Composable
private fun OrderTotal(
  orderCost: String,
  onConfirmationStep: Boolean,
  isVisible: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  if (!isVisible) return

  val bdColor =
    when {
      onConfirmationStep -> MaterialTheme.colorScheme.secondaryContainer
      else -> MaterialTheme.colorScheme.primaryContainer
    }
  val textColor =
    when {
      onConfirmationStep -> MaterialTheme.colorScheme.onSecondaryContainer
      else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

  var boxModifier =
    modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 32.dp)
      .padding(horizontal = 4.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(bdColor)
  if (onConfirmationStep) boxModifier = boxModifier.clickable(onClick = onClick)

  val label =
    when (onConfirmationStep) {
      true -> stringResource(R.string.bottom_bar_place_order)
      false -> stringResource(R.string.bottom_bar_order_total)
    }
  Box(modifier = boxModifier) {
    Text(
      text = label,
      modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
      color = textColor,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = "$$orderCost",
      modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
      color = textColor,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Preview
@Composable
internal fun PreviewOrderTacosUi() {
  val toppings =
    listOf(
      Ingredient("apple", calories = 10, diet = Diet.VEGAN),
      Ingredient("orange", calories = 15, diet = Diet.VEGETARIAN, charge = 199),
      Ingredient("pear", diet = Diet.NONE),
    )
  val stepState =
    ToppingsOrderStep.State.AvailableToppings(
      selected = setOf(toppings.first()),
      list = toppings,
      eventSink = {},
    )
  Surface {
    TacoTheme {
      OrderTacosUi(
        OrderTacosScreen.State(
          headerResId = R.string.toppings_step_header,
          orderCost = "1.99",
          stepState = stepState,
          isPreviousVisible = true,
          isNextVisible = true,
          isNextEnabled = false,
          isFooterVisible = true,
          eventSink = {},
        )
      )
    }
  }
}

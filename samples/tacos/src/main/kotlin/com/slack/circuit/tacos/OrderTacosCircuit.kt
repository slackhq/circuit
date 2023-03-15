package com.slack.circuit.tacos

import android.os.Parcelable
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.slack.circuit.CircuitContext
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.Ui
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.step.FillingsOrderStep
import com.slack.circuit.tacos.step.FillingsProducer
import com.slack.circuit.tacos.step.FillingsUi
import com.slack.circuit.tacos.step.OrderStep
import com.slack.circuit.tacos.step.ConfirmationOrderStep
import com.slack.circuit.tacos.step.ConfirmationProducer
import com.slack.circuit.tacos.step.ConfirmationUi
import com.slack.circuit.tacos.step.SummaryOrderStep
import com.slack.circuit.tacos.step.SummaryUi
import com.slack.circuit.tacos.step.ToppingsOrderStep
import com.slack.circuit.tacos.step.ToppingsProducer
import com.slack.circuit.tacos.step.ToppingsUi
import com.slack.circuit.tacos.step.summaryProducer
import com.slack.circuit.ui
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize object OrderTacosScreen : Screen {
  data class State(
    @StringRes val headerResId: Int,
    val orderCost: BigDecimal,
    val stepState: OrderStep.State,
    val isPreviousVisible: Boolean,
    val isNextEnabled: Boolean,
    val isNextVisible: Boolean,
    val isFooterVisible: Boolean,
    val eventSink: (Event) -> Unit
  ): CircuitUiState

  sealed interface Event : CircuitUiEvent {
    val value: Int
      get() = 0

    object Previous : Event { override val value: Int = -1 }
    object Next : Event { override val value: Int = 1 }
    object ProcessOrder : Event { override val value: Int = 1 }
  }
}

@Parcelize
@Immutable
data class OrderDetails(
  val filling: Ingredient = Ingredient(""),
  val toppings: Set<Ingredient> = persistentSetOf(),
  val baseCost: BigDecimal = BigDecimal("9.99"), // Default taco price
  val ingredientsCost: BigDecimal = BigDecimal.ZERO
): Parcelable

private val orderSteps = persistentListOf(
  FillingsOrderStep,
  ToppingsOrderStep,
  ConfirmationOrderStep,
  SummaryOrderStep
)

internal class OrderTacosPresenter(
  private val fillingsProducer: FillingsProducer,
  private val toppingsProducer: ToppingsProducer,
  private val confirmationProducer: ConfirmationProducer,
) : Presenter<OrderTacosScreen.State> {
  @Composable
  override fun present(): OrderTacosScreen.State = presentInternal()

  @Composable
  internal fun presentInternal(
    initialStep: OrderStep = FillingsOrderStep,
    initialOrderDetails: OrderDetails = OrderDetails(),
  ): OrderTacosScreen.State {
    var currentStep by rememberSaveable { mutableStateOf(initialStep) }
    var orderDetails by rememberSaveable { mutableStateOf(initialOrderDetails) }
    var isNextEnabled by remember { mutableStateOf(false) }

    val stepState = produceState(currentStep, orderDetails) { event ->
      when (event) {
        is OrderStep.Validation -> isNextEnabled = event.enabled
        is OrderStep.UpdateOrder -> orderDetails = updateOrder(event, orderDetails)
        is OrderStep.Restart -> {
          currentStep = initialStep
          orderDetails = initialOrderDetails
          isNextEnabled = false
        }
      }
    }

    return OrderTacosScreen.State(
      headerResId = currentStep.headerResId,
      orderCost = orderDetails.baseCost + orderDetails.ingredientsCost,
      stepState = stepState,
      isPreviousVisible = currentStep.number in 1..2,
      isNextEnabled = isNextEnabled,
      isNextVisible = currentStep.number < 2,
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
      is SummaryOrderStep -> summaryProducer(eventSink)
    }
}

private fun processNavigation(
  currentStep: OrderStep,
  navEvent: OrderTacosScreen.Event,
  onNavEvent: (OrderStep) -> Unit
) {
  val newIndex = currentStep.number + navEvent.value
  onNavEvent(orderSteps.getOrElse(newIndex) { currentStep })
}

private fun updateOrder(
  event: OrderStep.UpdateOrder,
  currentOrder: OrderDetails,
): OrderDetails {
  val newFilling = when (event) {
    is OrderStep.UpdateOrder.Filling -> event.ingredient
    else -> currentOrder.filling
  }
  val newToppings = when (event) {
    is OrderStep.UpdateOrder.Toppings -> event.ingredients
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

private fun calculateIngredientsCost(
  filling: Ingredient?,
  toppings: Set<Ingredient>
): BigDecimal {
  var cost = filling?.charge ?: BigDecimal.ZERO
  toppings.forEach { topping -> cost += topping.charge }
  return cost
}

@Composable
private fun OrderTacosUi(state: OrderTacosScreen.State, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  Scaffold(
    modifier = modifier.padding(5.dp),
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(state.headerResId)) },
        modifier = modifier,
        navigationIcon = {
          NavigationButton(
            direction = Direction.LEFT,
            visible = state.isPreviousVisible,
          ) { sink(OrderTacosScreen.Event.Previous) }
        },
        actions = {
          NavigationButton(
            direction = Direction.RIGHT,
            enabled = state.isNextEnabled,
            visible = state.isNextVisible,
          ) { sink(OrderTacosScreen.Event.Next) }
        }
      )
    },
    bottomBar = {
      OrderTotal(
        orderCost = state.orderCost,
        onConfirmationStep = state.stepState is ConfirmationOrderStep.Order,
        isVisible = state.isFooterVisible,
      ) { sink(OrderTacosScreen.Event.ProcessOrder) }
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

internal enum class Direction(
  val icon: ImageVector,
  @StringRes val descriptionResId: Int
) {
  LEFT(Icons.Filled.ArrowBack, R.string.top_bar_back),
  RIGHT(Icons.Filled.ArrowForward,R.string.top_bar_forward)
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

  val tintColour = when (enabled) {
    true -> MaterialTheme.colorScheme.onSurface
    false -> MaterialTheme.colorScheme.outline
  }

  IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
    Image(
      modifier = modifier,
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

  val color = when {
    onConfirmationStep -> MaterialTheme.colorScheme.onTertiaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer
  }
  var boxModifier = modifier
    .fillMaxWidth()
    .defaultMinSize(minHeight = 30.dp)
    .padding(horizontal = 5.dp)
    .clip(RoundedCornerShape(5.dp))
    .background(color)
  if (onConfirmationStep) boxModifier = boxModifier.clickable(onClick = onClick)

  val label = when (onConfirmationStep) {
    true -> stringResource(R.string.bottom_bar_place_order)
    false -> stringResource(R.string.bottom_bar_order_total)
  }
  Box(modifier = boxModifier) {
    Text(
      text = label,
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 5.dp),
      color = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = if (onConfirmationStep) "$$orderCost" else "$$orderCost",
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 5.dp),
      color = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.Bold,
    )
  }
}

internal class OrderTacosPresenterFactory(
  private val fillingsProducer: FillingsProducer,
  private val toppingsProducer: ToppingsProducer,
  private val confirmationProducer: ConfirmationProducer,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext
  ): Presenter<*>? =
    when (screen) {
      is OrderTacosScreen ->
        OrderTacosPresenter(fillingsProducer, toppingsProducer, confirmationProducer)
      else -> null
    }
}

internal class OrderTacosUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? =
    when (screen) {
      is OrderTacosScreen -> ui<OrderTacosScreen.State> { state, modifier ->
        OrderTacosUi(state, modifier)
      }
      else -> null
    }
}
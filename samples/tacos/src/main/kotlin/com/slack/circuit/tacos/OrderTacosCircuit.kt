package com.slack.circuit.tacos

import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.slack.circuit.CircuitContext
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.Ui
import com.slack.circuit.tacos.model.Cents
import com.slack.circuit.tacos.model.Ingredient
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
import com.slack.circuit.ui
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.Parcelize

@Parcelize object OrderTacosScreen : Screen {
  data class State(
    val headerText: String,
    val orderState: OrderStep.State,
    val isPreviousVisible: Boolean,
    val isNextEnabled: Boolean,
    val isNextVisible: Boolean,
    val eventSink: (Event) -> Unit
  ): CircuitUiState

  sealed interface Event : CircuitUiEvent {
    val value: Int
    object Previous : Event { override val value: Int = -1 }
    object Next : Event { override val value: Int = 1 }
  }
}

@Parcelize
@Immutable
data class OrderDetails(
  val filling: Ingredient? = null,
  val toppings: Set<Ingredient> = persistentSetOf(),
  val cost: Cents = 0
): Parcelable

private val orderSteps = persistentListOf(
  FillingsOrderStep,
  ToppingsOrderStep,
  SummaryOrderStep,
)

internal class OrderTacosPresenter(
  private val fillingsProducer: FillingsProducer,
  private val toppingsProducer: ToppingsProducer,
  private val summaryProducer: SummaryProducer,
) : Presenter<OrderTacosScreen.State> {
  @Composable
  override fun present(): OrderTacosScreen.State = presentInternal()

  @Composable
  internal fun presentInternal(
    initialStep: OrderStep = FillingsOrderStep,
    initialOrderDetails: OrderDetails = OrderDetails(),
  ): OrderTacosScreen.State {
    val currentStep = rememberSaveable { mutableStateOf(initialStep) }
    var orderDetails by rememberSaveable { mutableStateOf(initialOrderDetails) }
    var isNextEnabled by remember { mutableStateOf(false) }

    val stepState = currentStep.produceState(orderDetails) { event ->
      when (event) {
        is OrderStep.Validation -> isNextEnabled = event.enabled
        is OrderStep.UpdateOrder ->
          orderDetails = updateOrder(
            event = event,
            onNewFilling = { orderDetails.copy(filling = it) },
            onNewToppings = { orderDetails.copy(toppings = it.toImmutableSet()) }
          )
      }
    }

    return OrderTacosScreen.State(
      headerText = currentStep.value.headerText,
      orderState = stepState,
      isPreviousVisible = currentStep.value.number > 0,
      isNextEnabled = isNextEnabled,
      isNextVisible = currentStep.value.number < 2,
    ) { navEvent ->
      processNavigation(currentStep.value, navEvent) { currentStep.value = it }
    }
  }

  @Composable
  private fun MutableState<OrderStep>.produceState(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ): OrderStep.State =
    when (value) {
      is FillingsOrderStep -> fillingsProducer(orderDetails, eventSink)
      is ToppingsOrderStep -> toppingsProducer(orderDetails, eventSink)
      is SummaryOrderStep -> summaryProducer(orderDetails, eventSink)
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
  onNewFilling: (Ingredient) -> OrderDetails,
  onNewToppings: (Set<Ingredient>) -> OrderDetails,
): OrderDetails =
  when (event) {
    is OrderStep.UpdateOrder.Filling -> onNewFilling(event.ingredient)
    is OrderStep.UpdateOrder.Toppings -> onNewToppings(event.ingredients)
  }

@Composable
private fun OrderTacosUi(state: OrderTacosScreen.State, modifier: Modifier = Modifier) {
  val sink = state.eventSink
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(state.headerText) },
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
    }
  ) { padding ->
    val stepModifier = Modifier.padding(padding)
    when (state.orderState) {
      is FillingsOrderStep.State -> FillingsUi(state.orderState, stepModifier)
      is ToppingsOrderStep.State -> ToppingsUi(state.orderState, stepModifier)
      is SummaryOrderStep.Order -> SummaryUi(state.orderState, stepModifier)
    }
  }
}

enum class Direction(
  val icon: ImageVector,
  val description: String
) {
  LEFT(Icons.Filled.ArrowBack, "Back"),
  RIGHT(Icons.Filled.ArrowForward,"Forward")
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

  IconButton(modifier = modifier, enabled = enabled, onClick = onClick) {
    Image(
      modifier = modifier,
      painter = rememberVectorPainter(image = direction.icon),
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
      contentDescription = direction.description,
    )
  }
}

internal class OrderTacosPresenterFactory(
  private val fillingsProducer: FillingsProducer,
  private val toppingsProducer: ToppingsProducer,
  private val summaryProducer: SummaryProducer,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext
  ): Presenter<*>? =
    when (screen) {
      is OrderTacosScreen ->
        OrderTacosPresenter(fillingsProducer, toppingsProducer, summaryProducer)
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
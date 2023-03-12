package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object ToppingsOrderStep : OrderStep {
  @IgnoredOnParcel
  override val number = 1

  sealed interface State : OrderStep.State {
    object Loading : State
    data class AvailableToppings(
      val selected: ImmutableSet<Ingredient>,
      val list: ImmutableList<Ingredient>,
      val eventSink: (Event) -> Unit
    ) : State
  }

  sealed interface Event {
    data class AddTopping(val ingredient: Ingredient) : Event
    data class RemoveTopping(val ingredient: Ingredient) : Event
  }
}

private const val MINIMUM_TOPPINGS = 3

internal class ToppingsProducerImpl(private val repository: IngredientsRepository) : ToppingsProducer {
  @Composable
  override fun invoke(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ) = invoke(orderDetails, MINIMUM_TOPPINGS, eventSink)

  @Composable
  internal operator fun invoke(
    orderDetails: OrderDetails,
    minimumToppings: Int,
    eventSink: (OrderStep.Event) -> Unit
  ): OrderStep.State {
    val ingredients by produceState<ImmutableList<Ingredient>?>(null) {
      value = repository.getToppings()
    }

    validateToppings(orderDetails.toppings.size, minimumToppings, eventSink)
    return when (val list = ingredients) {
      null -> ToppingsOrderStep.State.Loading
      else ->
        ToppingsOrderStep.State.AvailableToppings(
          orderDetails.toppings.toImmutableSet(),
          list
        ) { event ->
          updateToppings(
            event = event,
            plusTopping = { orderDetails.toppings.plus(it) },
            minusTopping = { orderDetails.toppings.minus(it) },
            eventSink = eventSink
          )
        }
    }
  }
}

private fun updateToppings(
  event: ToppingsOrderStep.Event,
  plusTopping: (Ingredient) -> Set<Ingredient>,
  minusTopping: (Ingredient) -> Set<Ingredient>,
  eventSink: (OrderStep.Event) -> Unit
) {
  val updatedToppings = when (event) {
    is ToppingsOrderStep.Event.AddTopping -> plusTopping(event.ingredient)
    is ToppingsOrderStep.Event.RemoveTopping -> minusTopping(event.ingredient)
  }
  eventSink(OrderStep.UpdateOrder.Toppings(updatedToppings))
}

private fun validateToppings(selected: Int, minimum: Int, eventSink: (OrderStep.Event) -> Unit) {
  val validation = when {
    selected >= minimum -> OrderStep.Validation.Valid
    else -> OrderStep.Validation.Invalid
  }
  eventSink(validation)
}

@Composable
internal fun ToppingsUi(state: ToppingsOrderStep.State, modifier: Modifier = Modifier) {

}

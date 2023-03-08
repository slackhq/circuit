package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

object ToppingsOrderStep : OrderStep {
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

class ToppingsProducerImpl(private val repository: IngredientsRepository) : ToppingsProducer {
  @Composable
  override fun invoke(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ): OrderStep.State {
    val selected = remember {
      orderDetails.toppings.map { it to Unit }.toMutableStateMap()
    }
    val ingredients by produceState<ImmutableList<Ingredient>?>(null) {
      value = repository.getToppings()
    }

    return when (val list = ingredients) {
      null -> {
        eventSink(OrderStep.Validation.Invalid)
        ToppingsOrderStep.State.Loading
      }
      else ->
        ToppingsOrderStep.State.AvailableToppings(
          selected.keys.toImmutableSet(),
          list
        ) { event ->
          processToppingChange(
            event = event,
            addTopping = { selected[it] = Unit },
            removeTopping = { selected.remove(it) },
            selected = { selected.keys.toImmutableSet() },
            eventSink = eventSink
          )
        }
    }
  }
}

private fun processToppingChange(
  event: ToppingsOrderStep.Event,
  addTopping: (Ingredient) -> Unit,
  removeTopping: (Ingredient) -> Unit,
  selected: () -> ImmutableSet<Ingredient>,
  eventSink: (OrderStep.Event) -> Unit
) {
  when (event) {
    is ToppingsOrderStep.Event.AddTopping -> addTopping(event.ingredient)
    is ToppingsOrderStep.Event.RemoveTopping -> removeTopping(event.ingredient)
  }
  val set = selected()
  val validation = when {
    set.size >= 3 -> OrderStep.Validation.Valid
    else -> OrderStep.Validation.Invalid
  }

  eventSink(OrderStep.UpdateOrder.Toppings(set))
  eventSink(validation)
}

@Composable
fun ToppingsUi(state: ToppingsOrderStep.State, modifier: Modifier = Modifier) {

}

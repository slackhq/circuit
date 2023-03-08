package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableSet

object FillingsOrderStep : OrderStep {
  override val number = 0

  sealed interface State : OrderStep.State {
    object Loading : State
    data class AvailableFillings(
      val list: ImmutableList<Ingredient>,
      val eventSink: (Event) -> Unit
    ) : State
  }

  sealed interface Event {
    data class Selected(val ingredient: Ingredient) : Event
    data class Deselected(val ingredient: Ingredient) : Event
  }
}

class FillingsProducerImpl(private val repository: IngredientsRepository) : FillingsProducer {
  @Composable
  override fun invoke(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ): FillingsOrderStep.State {
    val selected = remember { mutableStateMapOf<Ingredient, Unit>() }
    val ingredients by produceState<ImmutableList<Ingredient>?>(null) {
      value = repository.getFillings()
    }

    return when (val data = ingredients) {
      null -> {
        eventSink(OrderStep.Validation.Invalid)
        FillingsOrderStep.State.Loading
      }
      else -> FillingsOrderStep.State.AvailableFillings(data) { event ->
        when (event) {
          is FillingsOrderStep.Event.Selected -> selected[event.ingredient] = Unit
          is FillingsOrderStep.Event.Deselected -> selected.remove(event.ingredient)
        }

        updateOrder(selected.keys, eventSink)
      }
    }
  }
}

private fun updateOrder(
  ingredients: Set<Ingredient>,
  eventSink: (OrderStep.Event) -> Unit
) {
  eventSink(OrderStep.UpdateOrder.Fillings(ingredients.toImmutableSet()))
  when {
    ingredients.isEmpty() -> eventSink(OrderStep.Validation.Invalid)
    else -> eventSink(OrderStep.Validation.Valid)
  }
}

@Composable
fun FillingsUi(state: FillingsProducer.State, modifier: Modifier = Modifier) {

}
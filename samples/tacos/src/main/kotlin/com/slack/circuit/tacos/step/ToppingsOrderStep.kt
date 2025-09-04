// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.R
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository

data object ToppingsOrderStep : OrderStep {
  override val index = 1
  override val headerResId = R.string.toppings_step_header

  sealed interface State : OrderStep.State {
    data object Loading : State

    data class AvailableToppings(
      val selected: Set<Ingredient>,
      val list: List<Ingredient>,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event {
    data class AddTopping(val ingredient: Ingredient) : Event

    data class RemoveTopping(val ingredient: Ingredient) : Event
  }
}

private const val MINIMUM_TOPPINGS = 3

internal class ToppingsProducerImpl(private val repository: IngredientsRepository) :
  OrderStep.StateProducer<ToppingsOrderStep.State> {
  @Composable
  override fun invoke(orderDetails: OrderDetails, eventSink: (OrderStep.Event) -> Unit) =
    invoke(orderDetails, MINIMUM_TOPPINGS, eventSink)

  @Composable
  internal operator fun invoke(
    orderDetails: OrderDetails,
    minimumToppings: Int,
    eventSink: (OrderStep.Event) -> Unit,
  ): ToppingsOrderStep.State {
    // False positive - https://issuetracker.google.com/issues/349411310
    @Suppress("ProduceStateDoesNotAssignValue")
    val ingredients by produceState<List<Ingredient>?>(null) { value = repository.getToppings() }

    validateToppings(orderDetails.toppings.size, minimumToppings, eventSink)
    return when (val list = ingredients) {
      null -> ToppingsOrderStep.State.Loading
      else ->
        ToppingsOrderStep.State.AvailableToppings(orderDetails.toppings, list) { event ->
          updateToppings(
            event = event,
            plusTopping = { orderDetails.toppings.plus(it) },
            minusTopping = { orderDetails.toppings.minus(it) },
            eventSink = eventSink,
          )
        }
    }
  }
}

private fun updateToppings(
  event: ToppingsOrderStep.Event,
  plusTopping: (Ingredient) -> Set<Ingredient>,
  minusTopping: (Ingredient) -> Set<Ingredient>,
  eventSink: (OrderStep.Event) -> Unit,
) {
  val updatedToppings =
    when (event) {
      is ToppingsOrderStep.Event.AddTopping -> plusTopping(event.ingredient)
      is ToppingsOrderStep.Event.RemoveTopping -> minusTopping(event.ingredient)
    }
  eventSink(OrderStep.UpdateOrder.SetToppings(updatedToppings))
}

private fun validateToppings(selected: Int, minimum: Int, eventSink: (OrderStep.Event) -> Unit) {
  val validation =
    when {
      selected >= minimum -> OrderStep.Validation.Valid
      else -> OrderStep.Validation.Invalid
    }
  eventSink(validation)
}

@Composable
internal fun ToppingsUi(state: ToppingsOrderStep.State, modifier: Modifier = Modifier) {
  when (state) {
    is ToppingsOrderStep.State.Loading -> Loading(modifier)
    is ToppingsOrderStep.State.AvailableToppings -> ToppingsList(state, modifier)
  }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ToppingsList(
  state: ToppingsOrderStep.State.AvailableToppings,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  Column(modifier = modifier.verticalScroll(scrollState).fillMaxWidth()) {
    Instructions(resId = R.string.toppings_step_instructions)
    state.list.forEach { ingredient ->
      Topping(
        ingredient = ingredient,
        isSelected = state.selected.contains(ingredient),
        onSelect = { selected ->
          val event =
            when {
              selected -> ToppingsOrderStep.Event::AddTopping
              else -> ToppingsOrderStep.Event::RemoveTopping
            }
          state.eventSink(event(ingredient))
        },
      )
    }
  }
}

@Composable
private fun Topping(
  ingredient: Ingredient,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onSelect: (Boolean) -> Unit,
) {
  Row(
    modifier = modifier.clickable { onSelect(!isSelected) }.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(checked = isSelected, modifier = Modifier, onCheckedChange = onSelect)
    OrderIngredient(ingredient)
  }
}

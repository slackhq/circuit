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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.R
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository

data object FillingsOrderStep : OrderStep {
  override val index = 0
  override val headerResId = R.string.fillings_step_header

  sealed interface State : OrderStep.State {
    data object Loading : State

    data class AvailableFillings(
      val selected: Ingredient? = null,
      val list: List<Ingredient>,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event {
    data class SelectFilling(val ingredient: Ingredient) : Event
  }
}

internal class FillingsProducerImpl(private val repository: IngredientsRepository) :
  OrderStep.StateProducer<FillingsOrderStep.State> {
  @Composable
  override fun invoke(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit,
  ): FillingsOrderStep.State {
    // False positive - https://issuetracker.google.com/issues/349411310
    @Suppress("ProduceStateDoesNotAssignValue")
    val ingredients by produceState<List<Ingredient>?>(null) { value = repository.getFillings() }

    validateFilling(orderDetails.filling, eventSink)
    return when (val list = ingredients) {
      null -> FillingsOrderStep.State.Loading
      else ->
        FillingsOrderStep.State.AvailableFillings(orderDetails.filling, list) { event ->
          when (event) {
            is FillingsOrderStep.Event.SelectFilling ->
              eventSink(OrderStep.UpdateOrder.SetFilling(event.ingredient))
          }
        }
    }
  }
}

private fun validateFilling(filling: Ingredient?, eventSink: (OrderStep.Event) -> Unit) {
  val validation =
    when (filling) {
      Ingredient("") -> OrderStep.Validation.Invalid
      else -> OrderStep.Validation.Valid
    }
  eventSink(validation)
}

@Composable
internal fun FillingsUi(state: FillingsOrderStep.State, modifier: Modifier = Modifier) {
  when (state) {
    is FillingsOrderStep.State.Loading -> Loading(modifier)
    is FillingsOrderStep.State.AvailableFillings -> FillingsList(state, modifier)
  }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun FillingsList(
  state: FillingsOrderStep.State.AvailableFillings,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  Column(modifier = modifier.verticalScroll(scrollState).fillMaxWidth()) {
    Instructions(resId = R.string.fillings_step_instructions)
    state.list.forEach { ingredient ->
      Filling(
        ingredient = ingredient,
        isSelected = state.selected == ingredient,
        onSelect = { state.eventSink(FillingsOrderStep.Event.SelectFilling(ingredient)) },
      )
    }
  }
}

@Composable
private fun Filling(
  ingredient: Ingredient,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onSelect: () -> Unit,
) {
  Row(
    modifier = modifier.clickable(onClick = onSelect).fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = isSelected, onClick = onSelect)
    OrderIngredient(ingredient)
  }
}

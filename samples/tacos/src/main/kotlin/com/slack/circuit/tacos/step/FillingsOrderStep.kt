// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.R
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object FillingsOrderStep : OrderStep {
  @IgnoredOnParcel override val number = 0
  @IgnoredOnParcel override val headerResId = R.string.fillings_step_header

  sealed interface State : OrderStep.State {
    object Loading : State
    data class AvailableFillings(
      val selected: Ingredient? = null,
      val list: ImmutableList<Ingredient>,
      val eventSink: (Event) -> Unit
    ) : State
  }

  sealed interface Event {
    data class SelectFilling(val ingredient: Ingredient) : Event
  }
}

internal class FillingsProducerImpl(private val repository: IngredientsRepository) :
  FillingsProducer {
  @Composable
  override fun invoke(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ): FillingsOrderStep.State {
    val ingredients by
      produceState<ImmutableList<Ingredient>?>(null) { value = repository.getFillings() }

    validateFilling(orderDetails.filling, eventSink)
    return when (val list = ingredients) {
      null -> FillingsOrderStep.State.Loading
      else ->
        FillingsOrderStep.State.AvailableFillings(orderDetails.filling, list) { event ->
          when (event) {
            is FillingsOrderStep.Event.SelectFilling ->
              eventSink(OrderStep.UpdateOrder.Filling(event.ingredient))
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
    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun FillingsList(
  state: FillingsOrderStep.State.AvailableFillings,
  modifier: Modifier = Modifier
) {
  val sink = state.eventSink
  val scrollState = rememberScrollState()
  Column(modifier = modifier.verticalScroll(scrollState)) {
    Instructions(resId = R.string.fillings_step_instructions)
    state.list.forEach { ingredient ->
      Filling(
        ingredient = ingredient,
        isSelected = state.selected == ingredient,
        onSelect = { sink(FillingsOrderStep.Event.SelectFilling(ingredient)) },
      )
    }
  }
}

@Composable
private fun Filling(
  ingredient: Ingredient,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onSelect: () -> Unit
) {
  Row(
    modifier = modifier.clickable(onClick = onSelect),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(selected = isSelected, modifier = modifier, onClick = onSelect)
    Column {
      with(ingredient) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(name)
          DietBadge(diet, Modifier.padding(5.dp))
        }
        Row {
          AdditionalCharge(charge)
          ExtrasDivider(charge, calories)
          Calories(calories)
        }
      }
    }
  }
}

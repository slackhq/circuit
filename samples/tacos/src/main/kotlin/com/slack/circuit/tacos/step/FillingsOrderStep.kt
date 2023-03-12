package com.slack.circuit.tacos.step

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Cents
import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@Parcelize
object FillingsOrderStep : OrderStep {
  @IgnoredOnParcel
  override val number = 0

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

class FillingsProducerImpl(private val repository: IngredientsRepository) : FillingsProducer {
  @Composable
  override fun invoke(
    orderDetails: OrderDetails,
    eventSink: (OrderStep.Event) -> Unit
  ): FillingsOrderStep.State {
    val ingredients by produceState<ImmutableList<Ingredient>?>(null) {
      value = repository.getFillings()
    }

    validateFilling(orderDetails.filling, eventSink)
    return when (val list = ingredients) {
      null -> FillingsOrderStep.State.Loading
      else ->
        FillingsOrderStep.State.AvailableFillings(
          orderDetails.filling,
          list
        ) { event ->
          when (event) {
            is FillingsOrderStep.Event.SelectFilling ->
              eventSink(OrderStep.UpdateOrder.Filling(event.ingredient))
          }
        }
    }
  }
}

private fun validateFilling(filling: Ingredient?, eventSink: (OrderStep.Event) -> Unit) {
  val validation = when(filling) {
    null -> OrderStep.Validation.Invalid
    else -> OrderStep.Validation.Valid
  }
  eventSink(validation)
}

@Composable
fun FillingsUi(state: FillingsOrderStep.State, modifier: Modifier = Modifier) {
  when (state) {
    is FillingsOrderStep.State.Loading -> Text("loading...", modifier)
    is FillingsOrderStep.State.AvailableFillings -> FillingsList(state, modifier)
  }
}

@Composable
private fun FillingsList(
  state: FillingsOrderStep.State.AvailableFillings,
  modifier: Modifier = Modifier
) {
  val sink = state.eventSink
  Column(modifier = modifier) {
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
  Row(modifier = modifier) {
    RadioButton(selected = isSelected, modifier = modifier, onClick = onSelect)
    Column {
      with(ingredient) {
        Row {
          Text(name)
          DietBadge(diet)
        }
        AdditionalCharge(charge)
        Spacer(charge, calories)
        Calories(calories)
      }
    }
  }
}

@Composable
private fun DietBadge(diet: Diet, modifier: Modifier = Modifier) {
  if (diet == Diet.NONE) return
  Text(diet.name.lowercase(), modifier)
}

@Composable
private fun AdditionalCharge(charge: Cents, modifier: Modifier = Modifier) {
  if (charge <= 0) return
  val dollarAmount = BigDecimal(charge) / BigDecimal(100)
  Text("$$dollarAmount", modifier)
}

@Composable
private fun Calories(calories: Int, modifier: Modifier = Modifier) {
  if (calories <= 0) return
  Text("$calories cals", modifier)
}

@Composable
private fun Spacer(charge: Cents, calories: Int, modifier: Modifier = Modifier) {
  if (charge <= 0 || calories <= 0) return
  Text(" | ", modifier)
}

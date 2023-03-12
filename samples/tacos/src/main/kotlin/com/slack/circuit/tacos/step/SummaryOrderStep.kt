package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object SummaryOrderStep : OrderStep {
  @IgnoredOnParcel override val number = 2
  @IgnoredOnParcel override val headerText = "Order Summary"

  data class Order(
    val filling: Ingredient?,
    val toppings: ImmutableSet<Ingredient>,
  ) : OrderStep.State
}

@Composable
internal fun summaryProducer(orderDetails: OrderDetails): SummaryOrderStep.Order {
  return SummaryOrderStep.Order(
    filling = orderDetails.filling,
    toppings = orderDetails.toppings.toImmutableSet(),
  )
}

@Composable
internal fun SummaryUi(state: SummaryOrderStep.Order, modifier: Modifier = Modifier) {

}

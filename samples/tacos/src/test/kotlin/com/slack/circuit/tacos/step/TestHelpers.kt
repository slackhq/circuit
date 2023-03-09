package com.slack.circuit.tacos.step

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

internal inline fun <reified T : Any> OrderStep.State.asInstanceOf(): T =
  (this as? T) ?: error("unable to cast $this to ${T::class.simpleName}")

internal class FakeOrderStepParent(
  filling: Ingredient? = null,
  toppings: ImmutableSet<Ingredient> = persistentSetOf(),
) {
  private val validations = ArrayDeque<OrderStep.Validation>()
  var orderDetails by mutableStateOf(
    OrderDetails(
      filling = filling,
      toppings = toppings,
    )
  )
    private set

  fun childEvent(event: OrderStep.Event) {
    when (event) {
      is OrderStep.Validation -> validations.addLast(event)
      is OrderStep.UpdateOrder.Filling ->
        orderDetails = orderDetails.copy(filling = event.ingredient)
      is OrderStep.UpdateOrder.Toppings ->
        orderDetails = orderDetails.copy(toppings = event.ingredients)
    }
  }

  fun assertValidation(expected: OrderStep.Validation) {
    validations.isNotEmpty() || error("No unprocessed validations!")
    assertThat(validations.removeFirst()).isEqualTo(expected)
  }
}


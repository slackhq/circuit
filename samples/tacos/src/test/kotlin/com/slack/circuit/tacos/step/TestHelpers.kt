// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient

internal inline fun <reified T : Any> OrderStep.State.asInstanceOf(): T =
  (this as? T) ?: error("unable to cast $this to ${T::class.simpleName}")

internal class FakeOrderStepParent(
  filling: Ingredient = Ingredient(""),
  toppings: Set<Ingredient> = emptySet(),
) {
  private val validations = ArrayDeque<OrderStep.Validation>()
  var orderDetails by mutableStateOf(OrderDetails(filling = filling, toppings = toppings))
    private set

  fun childEvent(event: OrderStep.Event) {
    when (event) {
      is OrderStep.Validation -> validations.addLast(event)
      is OrderStep.UpdateOrder.SetFilling ->
        orderDetails = orderDetails.copy(filling = event.ingredient)
      is OrderStep.UpdateOrder.SetToppings ->
        orderDetails = orderDetails.copy(toppings = event.ingredients)
      is OrderStep.Restart -> orderDetails = OrderDetails()
    }
  }

  fun assertValidation(expected: OrderStep.Validation): FakeOrderStepParent = apply {
    validations.isNotEmpty() || error("No unprocessed validations!")
    assertThat(validations.removeFirst()).isEqualTo(expected)
  }

  fun assertNoUnconsumedValidation(): FakeOrderStepParent = apply {
    assertThat(validations).isEmpty()
  }
}

internal val testFillings =
  listOf(
    Ingredient("apple", calories = 10, charge = 199, diet = Diet.VEGAN),
    Ingredient("orange", diet = Diet.VEGETARIAN),
    Ingredient("pear", diet = Diet.NONE),
  )

internal val testToppings =
  listOf(
    Ingredient("carrot", calories = 15, charge = 75, diet = Diet.VEGAN),
    Ingredient("onion", diet = Diet.VEGETARIAN),
    Ingredient("potato", diet = Diet.NONE),
  )

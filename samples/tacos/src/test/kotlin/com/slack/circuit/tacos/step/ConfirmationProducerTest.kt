// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Diet
import com.slack.circuit.tacos.model.Ingredient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfirmationProducerTest {
  @Test
  fun `confirmationProducer - emitted state contains sorted toppings`() = runTest {
    val details = OrderDetails(toppings = testToppings.reversed().toSet())
    val expectedToppings = testToppings.map { it.name }

    moleculeFlow(RecompositionMode.Immediate) { confirmationProducer(details) }
      .test { awaitItem().run { assertThat(toppings).isEqualTo(expectedToppings) } }
  }

  @Test
  fun `confirmationProducer - emitted state contains expected calorie count`() = runTest {
    val filling = testFillings.first()
    val details = OrderDetails(filling = filling, toppings = testToppings.toSet())
    val expectedCalorieCount =
      filling.calories + testToppings.fold(0) { acc, topping -> acc + topping.calories }

    moleculeFlow(RecompositionMode.Immediate) { confirmationProducer(details) }
      .test { awaitItem().run { assertThat(calories).isEqualTo(expectedCalorieCount) } }
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
class ConfirmationProducerDietTest(
  private val expectedDiet: Diet,
  private val filling: Ingredient,
  private val toppings: List<Ingredient>,
) {
  @Test
  fun `confirmationProducer - emitted state contains expected diet`() = runTest {
    val details = OrderDetails(filling = filling, toppings = toppings.toSet())

    moleculeFlow(RecompositionMode.Immediate) { confirmationProducer(details) }
      .test { awaitItem().run { assertThat(diet).isEqualTo(expectedDiet) } }
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters
    fun data() =
      listOf(
        arrayOf(Diet.VEGAN, testFillings.first(), listOf(testToppings.first())),
        arrayOf(Diet.VEGETARIAN, testFillings.first(), testToppings.subList(0, 2)),
        arrayOf(Diet.NONE, testFillings.first(), testToppings),
        arrayOf(Diet.NONE, testFillings.last(), testToppings.subList(0, 2)),
      )
  }
}

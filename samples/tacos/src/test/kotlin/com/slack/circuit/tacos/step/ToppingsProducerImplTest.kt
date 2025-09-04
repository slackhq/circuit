// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToppingsProducerImplTest {
  @Test
  fun `invoke - emit loading then AvailableToppings`() = runTest {
    val parent = FakeOrderStepParent()
    val repo = TestIngredientsRepository(testToppings)
    val producer = ToppingsProducerImpl(repo)

    moleculeFlow(RecompositionMode.Immediate) {
        producer(orderDetails = parent.orderDetails, eventSink = parent::childEvent)
      }
      .test {
        assertThat(awaitItem()).isEqualTo(ToppingsOrderStep.State.Loading)
        parent.assertValidation(OrderStep.Validation.Invalid)

        awaitItem().asInstanceOf<ToppingsOrderStep.State.AvailableToppings>().run {
          assertThat(selected).isEmpty()
          assertThat(list).isEqualTo(testToppings)
        }
        parent.assertValidation(OrderStep.Validation.Invalid).assertNoUnconsumedValidation()
      }
  }

  @Test
  fun `invoke - emit Valid validation event after adding minimum toppings`() = runTest {
    val parent = FakeOrderStepParent()
    val repo = TestIngredientsRepository(testToppings)
    val producer = ToppingsProducerImpl(repo)

    moleculeFlow(RecompositionMode.Immediate) {
        producer(
          orderDetails = parent.orderDetails,
          minimumToppings = 1,
          eventSink = parent::childEvent,
        )
      }
      .test {
        // Loading
        awaitItem()
        parent.assertValidation(OrderStep.Validation.Invalid)

        // Initial AvailableToppings
        awaitItem()
          .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
          .also { parent.assertValidation(OrderStep.Validation.Invalid) }
          .run {
            assertThat(selected).isEmpty()

            // select first topping
            eventSink(ToppingsOrderStep.Event.AddTopping(testToppings[0]))
          }

        // New AvailableToppings containing toppings[0]
        awaitItem().asInstanceOf<ToppingsOrderStep.State.AvailableToppings>().run {
          assertThat(selected).isEqualTo(setOf(testToppings[0]))
        }
        parent.assertValidation(OrderStep.Validation.Valid).assertNoUnconsumedValidation()
      }
  }

  @Test
  fun `invoke - emit Invalid validation event after removing topping and falling below minimum`() =
    runTest {
      val parent = FakeOrderStepParent(toppings = setOf(testToppings[0]))
      val repo = TestIngredientsRepository(testToppings)
      val producer = ToppingsProducerImpl(repo)

      moleculeFlow(RecompositionMode.Immediate) {
          producer(
            orderDetails = parent.orderDetails,
            minimumToppings = 1,
            eventSink = parent::childEvent,
          )
        }
        .test {
          // Loading
          awaitItem()
          parent.assertValidation(OrderStep.Validation.Valid)

          // Initial AvailableToppings containing toppings[0]
          awaitItem()
            .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
            .also { parent.assertValidation(OrderStep.Validation.Valid) }
            .run {
              assertThat(selected).isEqualTo(setOf(testToppings[0]))

              eventSink(ToppingsOrderStep.Event.RemoveTopping(testToppings[0]))
            }

          // New AvailableToppings with topping count below minimum
          awaitItem()
            .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
            .also {
              parent.assertValidation(OrderStep.Validation.Invalid).assertNoUnconsumedValidation()
            }
            .run { assertThat(selected).isEmpty() }
        }
    }
}

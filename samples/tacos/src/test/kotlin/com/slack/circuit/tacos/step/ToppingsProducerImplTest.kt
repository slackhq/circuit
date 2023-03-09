package com.slack.circuit.tacos.step

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToppingsProducerImplTest {
  @Test
  fun `invoke - emit loading then AvailableToppings`() = runTest {
    val parent = FakeOrderStepParent()
    val repo = TestIngredientsRepository(toppings)
    val producer = ToppingsProducerImpl(repo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = parent.orderDetails,
        eventSink = parent::childEvent
      )
    }.test {
      assertThat(awaitItem()).isEqualTo(ToppingsOrderStep.State.Loading)
      parent.assertValidation(OrderStep.Validation.Invalid)

      awaitItem()
        .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
        .run {
          assertThat(selected).isEmpty()
          assertThat(list).isEqualTo(toppings)
        }
      parent.assertValidation(OrderStep.Validation.Invalid)
    }
  }

  @Test
  fun `invoke - emit Valid validation event after adding minimum toppings`() = runTest {
    val parent = FakeOrderStepParent()
    val repo = TestIngredientsRepository(toppings)
    val producer = ToppingsProducerImpl(repo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = parent.orderDetails,
        minimumToppings = 1,
        eventSink = parent::childEvent
      )
    }.test {
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
          eventSink(ToppingsOrderStep.Event.AddTopping(toppings[0]))
        }

      // New AvailableToppings containing toppings[0]
      awaitItem()
        .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
        .run {
          assertThat(selected).isEqualTo(persistentSetOf(toppings[0]))
        }
      parent.assertValidation(OrderStep.Validation.Valid)
    }
  }

  @Test
  fun `invoke - emit Invalid validation event after removing topping and falling below minimum`() =
    runTest {
      val parent = FakeOrderStepParent(toppings = persistentSetOf(toppings[0]))
      val repo = TestIngredientsRepository(toppings)
      val producer = ToppingsProducerImpl(repo)

      moleculeFlow(RecompositionClock.Immediate) {
        producer(
          orderDetails = parent.orderDetails,
          minimumToppings = 1,
          eventSink = parent::childEvent
        )
      }.test {
        // Loading
        awaitItem()
        parent.assertValidation(OrderStep.Validation.Valid)

        // Initial AvailableToppings containing toppings[0]
        awaitItem()
          .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
          .also { parent.assertValidation(OrderStep.Validation.Valid) }
          .run {
            assertThat(selected).isEqualTo(persistentSetOf(toppings[0]))

            eventSink(ToppingsOrderStep.Event.RemoveTopping(toppings[0]))
          }

        // New AvailableToppings with topping count below minimum
        awaitItem()
          .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
          .also { parent.assertValidation(OrderStep.Validation.Invalid) }
          .run {
            assertThat(selected).isEmpty()
          }
      }
    }
}

private val toppings = persistentListOf(
  Ingredient("carrot"),
  Ingredient("onion"),
  Ingredient("potato"),
)


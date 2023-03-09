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
    val actualEvents = ArrayDeque<OrderStep.Event>()
    val repo = TestIngredientsRepository(toppings)
    val producer = ToppingsProducerImpl(repo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = OrderDetails(),
        eventSink = { actualEvents.addLast(it) }
      )
    }.test {
      assertThat(awaitItem()).isEqualTo(ToppingsOrderStep.State.Loading)
      assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Invalid)

      awaitItem()
        .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
        .run {
          assertThat(selected).isEmpty()
          assertThat(list).isEqualTo(toppings)
        }
      assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Invalid)
    }
  }

  @Test
  fun `invoke - emit Valid validation event after adding minimum toppings`() = runTest {
    val actualEvents = ArrayDeque<OrderStep.Event>()
    val repo = TestIngredientsRepository(toppings)
    val producer = ToppingsProducerImpl(repo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = OrderDetails(),
        minimumToppings = 1,
        eventSink = { actualEvents.addLast(it) }
      )
    }.test {
      // Loading
      awaitItem()
      assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Invalid)

      // Initial AvailableToppings
      awaitItem()
        .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
        .also { assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Invalid) }
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
      assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Valid)
    }
  }

  @Test
  fun `invoke - emit Invalid validation event after removing topping and falling below minimum`() =
    runTest {
      val actualEvents = ArrayDeque<OrderStep.Event>()
      val repo = TestIngredientsRepository(toppings)
      val producer = ToppingsProducerImpl(repo)

      moleculeFlow(RecompositionClock.Immediate) {
        producer(
          orderDetails = OrderDetails(
            toppings = persistentSetOf(toppings[0]),
          ),
          minimumToppings = 1,
          eventSink = { actualEvents.addLast(it) }
        )
      }.test {
        // Loading
        awaitItem()
        assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Invalid)

        // Initial AvailableToppings containing toppings[0]
        awaitItem()
          .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
          .also { assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Valid) }
          .run {
            assertThat(selected).isEqualTo(persistentSetOf(toppings[0]))

            eventSink(ToppingsOrderStep.Event.RemoveTopping(toppings[0]))
          }

        // New AvailableToppings with topping count below minimum
        awaitItem()
          .asInstanceOf<ToppingsOrderStep.State.AvailableToppings>()
          .also { assertThat(actualEvents.removeFirst()).isEqualTo(OrderStep.Validation.Invalid) }
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


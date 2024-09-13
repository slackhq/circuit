// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.model.toCurrencyString
import com.slack.circuit.tacos.step.FillingsOrderStep
import com.slack.circuit.tacos.step.OrderStep
import com.slack.circuit.tacos.step.SummaryOrderStep
import com.slack.circuit.tacos.step.ToppingsOrderStep
import com.slack.circuit.test.presenterTestOf
import com.slack.circuit.test.test
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrderTacosPresenterTest {
  @Test
  fun `present - emits FillingsOrderStep state on initial composition`() = runTest {
    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
        toppingsProducer = { _, _ -> error("wrong step") },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
      )

    presenter.test {
      awaitItem().run {
        assertThat(stepState).isEqualTo(FillingsOrderStep.State.Loading)
        assertThat(isPreviousVisible).isFalse()
        assertThat(isNextEnabled).isFalse()
        assertThat(isNextVisible).isTrue()
      }
    }
  }

  @Test
  fun `present - changes to next OrderStep on navigation event`() = runTest {
    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
        toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
      )

    presenter.test {
      awaitItem().run {
        assertThat(stepState).isEqualTo(FillingsOrderStep.State.Loading)
        eventSink(OrderTacosScreen.Event.Next)
      }
      assertThat(awaitItem().stepState).isEqualTo(ToppingsOrderStep.State.Loading)
    }
  }

  @Test
  fun `present - changes to previous OrderStep on navigation event`() = runTest {
    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
        toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
        initialStep = ToppingsOrderStep,
      )

    presenterTestOf({ presenter.present() }) {
      awaitItem().run { eventSink(OrderTacosScreen.Event.Previous) }
      assertThat(awaitItem().stepState).isEqualTo(FillingsOrderStep.State.Loading)
    }
  }

  @Test
  fun `present - toggles next button based on OrderStep validation event`() = runTest {
    lateinit var sink: (OrderStep.Event) -> Unit
    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { _, eventSink ->
          sink = eventSink
          FillingsOrderStep.State.Loading
        },
        toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
      )

    presenter.test {
      assertThat(awaitItem().isNextEnabled).isFalse()
      sink(OrderStep.Validation.Valid)
      assertThat(awaitItem().isNextEnabled).isTrue()
      sink(OrderStep.Validation.Invalid)
      assertThat(awaitItem().isNextEnabled).isFalse()
    }
  }

  @Test
  fun `present - updates OrderDetails after receiving new filling from OrderStep`() = runTest {
    lateinit var sink: (OrderStep.Event) -> Unit
    lateinit var details: OrderDetails

    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { orderDetails, eventSink ->
          details = orderDetails
          sink = eventSink
          FillingsOrderStep.State.Loading
        },
        toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
      )

    presenter.test {
      awaitItem()
      assertThat(details).isEqualTo(OrderDetails())

      val filling = Ingredient("apple")
      sink(OrderStep.UpdateOrder.SetFilling(filling))

      advanceUntilIdle()
      assertThat(details).isEqualTo(OrderDetails(filling = filling))
    }
  }

  @Test
  fun `present - updates OrderDetails after receiving new toppings from OrderStep`() = runTest {
    lateinit var sink: (OrderStep.Event) -> Unit
    lateinit var details: OrderDetails

    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
        toppingsProducer = { orderDetails, eventSink ->
          details = orderDetails
          sink = eventSink
          ToppingsOrderStep.State.Loading
        },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
        initialStep = ToppingsOrderStep,
      )

    presenterTestOf({ presenter.present() }) {
      awaitItem()
      assertThat(details).isEqualTo(OrderDetails())

      val toppings = persistentSetOf(Ingredient("apple"))
      sink(OrderStep.UpdateOrder.SetToppings(toppings))

      assertThat(awaitItem().stepState).isEqualTo(ToppingsOrderStep.State.Loading)
      assertThat(details).isEqualTo(OrderDetails(toppings = toppings))
    }
  }

  @Test
  fun `present - resets all state after receiving restart event`() = runTest {
    lateinit var sink: (OrderStep.Event) -> Unit
    lateinit var details: OrderDetails

    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { orderDetails, _ ->
          details = orderDetails
          FillingsOrderStep.State.Loading
        },
        toppingsProducer = { _, _ -> error("wrong step") },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, eventSink ->
          sink = eventSink
          SummaryOrderStep.SummaryState {}
        },
        initialStep = SummaryOrderStep,
      )

    presenterTestOf({ presenter.present() }) {
      assertThat(awaitItem().stepState).isInstanceOf(SummaryOrderStep.SummaryState::class.java)

      sink(OrderStep.Restart)

      awaitItem().run {
        assertThat(stepState).isEqualTo(FillingsOrderStep.State.Loading)
        assertThat(isNextEnabled).isFalse()
      }

      assertThat(details).isEqualTo(OrderDetails())
    }
  }

  @Test
  fun `present - update order cost when ingredients change`() = runTest {
    val initialData = OrderDetails(ingredientsCost = 199)
    val expectedCost = initialData.run { baseCost + ingredientsCost }

    val presenter =
      OrderTacosPresenter(
        fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
        toppingsProducer = { _, _ -> error("wrong step") },
        confirmationProducer = { _, _ -> error("wrong step") },
        summaryProducer = { _, _ -> error("wrong step") },
        initialOrderDetails = initialData,
      )

    presenterTestOf({ presenter.present() }) {
      awaitItem().run { assertThat(orderCost).isEqualTo(expectedCost.toCurrencyString()) }
    }
  }
}

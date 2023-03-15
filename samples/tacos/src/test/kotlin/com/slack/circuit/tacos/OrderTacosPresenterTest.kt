package com.slack.circuit.tacos

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.step.FillingsOrderStep
import com.slack.circuit.tacos.step.OrderStep
import com.slack.circuit.tacos.step.ToppingsOrderStep
import com.slack.circuit.test.test
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrderTacosPresenterTest {
  @Test
  fun `present - emits FillingsOrderStep state on initial composition`() = runTest {
    val presenter = OrderTacosPresenter(
      fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
      toppingsProducer = { _, _ -> error("wrong step") },
      confirmationProducer = { _, _ -> error("wrong step") }
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
    val presenter = OrderTacosPresenter(
      fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
      toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
      confirmationProducer = { _, _ -> error("wrong step") }
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
    val presenter = OrderTacosPresenter(
      fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
      toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
      confirmationProducer = { _, _ -> error("wrong step") }
    )

    moleculeFlow(RecompositionClock.Immediate) {
      presenter.presentInternal(ToppingsOrderStep)
    }.test {
      awaitItem().run {
        assertThat(stepState).isEqualTo(ToppingsOrderStep.State.Loading)
        eventSink(OrderTacosScreen.Event.Previous)
      }
      assertThat(awaitItem().stepState).isEqualTo(FillingsOrderStep.State.Loading)
    }
  }

  @Test
  fun `present - do nothing if navigation event does not make sense`() = runTest {
    val presenter = OrderTacosPresenter(
      fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
      toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
      confirmationProducer = { _, _ -> error("wrong step") }
    )

    presenter.test {
      awaitItem().run {
        assertThat(stepState).isEqualTo(FillingsOrderStep.State.Loading)
        eventSink(OrderTacosScreen.Event.Previous)
      }

      // navigation invalid; should not recompose
      expectNoEvents()
    }
  }

  @Test
  fun `present - toggles next button based on OrderStep validation event`() = runTest {
    lateinit var sink: (OrderStep.Event) -> Unit
    val presenter = OrderTacosPresenter(
      fillingsProducer = { _, eventSink ->
        sink = eventSink
        FillingsOrderStep.State.Loading
      },
      toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
      confirmationProducer = { _, _ -> error("wrong step") }
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

    val presenter = OrderTacosPresenter(
      fillingsProducer = { orderDetails, eventSink ->
        details = orderDetails
        sink = eventSink
        FillingsOrderStep.State.Loading
      },
      toppingsProducer = { _, _ -> ToppingsOrderStep.State.Loading },
      confirmationProducer = { _, _ -> error("wrong step") }
    )

    presenter.test {
      assertThat(awaitItem().stepState).isEqualTo(FillingsOrderStep.State.Loading)
      assertThat(details).isEqualTo(OrderDetails())

      val filling = Ingredient("apple")
      sink(OrderStep.UpdateOrder.Filling(filling))

      assertThat(awaitItem().stepState).isEqualTo(FillingsOrderStep.State.Loading)
      assertThat(details).isEqualTo(OrderDetails(filling = filling))
    }
  }

  @Test
  fun `present - updates OrderDetails after receiving new toppings from OrderStep`() = runTest {
    lateinit var sink: (OrderStep.Event) -> Unit
    lateinit var details: OrderDetails

    val presenter = OrderTacosPresenter(
      fillingsProducer = { _, _ -> FillingsOrderStep.State.Loading },
      toppingsProducer = { orderDetails, eventSink ->
        details = orderDetails
        sink = eventSink
        ToppingsOrderStep.State.Loading
      },
      confirmationProducer = { _, _ -> error("wrong step") }
    )

    moleculeFlow(RecompositionClock.Immediate) {
      presenter.presentInternal(ToppingsOrderStep)
    }.test {
      assertThat(awaitItem().stepState).isEqualTo(ToppingsOrderStep.State.Loading)
      assertThat(details).isEqualTo(OrderDetails())

      val toppings = persistentSetOf(Ingredient("apple"))
      sink(OrderStep.UpdateOrder.Toppings(toppings))

      assertThat(awaitItem().stepState).isEqualTo(ToppingsOrderStep.State.Loading)
      assertThat(details).isEqualTo(OrderDetails(toppings = toppings))
    }
  }
}

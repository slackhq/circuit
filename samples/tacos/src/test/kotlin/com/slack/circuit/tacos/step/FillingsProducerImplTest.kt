package com.slack.circuit.tacos.step

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.model.Ingredient
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FillingsProducerImplTest {
  @Test
  fun `invoke - emit Loading then AvailableFillings`() = runTest {
    val parent = FakeOrderStepParent()
    val repo = TestIngredientsRepository(fillings)
    val producer = FillingsProducerImpl(repo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = parent.orderDetails,
        eventSink = parent::childEvent
      )
    }.test {
      assertThat(awaitItem()).isEqualTo(FillingsOrderStep.State.Loading)
      parent.assertValidation(OrderStep.Validation.Invalid)

      awaitItem()
        .asInstanceOf<FillingsOrderStep.State.AvailableFillings>()
        .run {
          assertThat(selected).isNull()
          assertThat(list).isEqualTo(fillings)
        }
      parent
        .assertValidation(OrderStep.Validation.Invalid)
        .assertNoUnconsumedValidation()
    }

  }

  @Test
  fun `invoke - emit Valid validation event after selecting filling`() = runTest {
    val parent = FakeOrderStepParent()
    val repo = TestIngredientsRepository(fillings)
    val producer = FillingsProducerImpl(repo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = parent.orderDetails,
        eventSink = parent::childEvent
      )
    }.test {
      awaitItem() // Loading
      parent.assertValidation(OrderStep.Validation.Invalid)

      // select a filling
      val expectedFilling = fillings[0]
      awaitItem()
        .asInstanceOf<FillingsOrderStep.State.AvailableFillings>()
        .also { parent.assertValidation(OrderStep.Validation.Invalid) }
        .run {
          assertThat(selected).isNull()
          eventSink(FillingsOrderStep.Event.SelectFilling(expectedFilling))
        }

      // verify filling is selected
      awaitItem()
        .asInstanceOf<FillingsOrderStep.State.AvailableFillings>()
        .also {
          parent
            .assertValidation(OrderStep.Validation.Valid)
            .assertNoUnconsumedValidation()
        }
        .run {
          assertThat(selected).isEqualTo(expectedFilling)
        }
    }
  }
}

private val fillings = persistentListOf(
  Ingredient("apple"),
  Ingredient("orange"),
  Ingredient("pear"),
)


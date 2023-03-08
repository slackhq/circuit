package com.slack.circuit.tacos.step

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.repository.IngredientsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FillingsProducerImplTest {
  @Test
  fun `invoke - emit Loading then AvailableFillings`() = runTest {
    val actualValidationEvents = mutableListOf<OrderStep.Event>()
    val expected = persistentListOf(
      Ingredient("apple"),
      Ingredient("orange"),
      Ingredient("pear"),
    )
    val testRepop = TestRepo(expected)
    val producer = FillingsProducerImpl(testRepop)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = OrderDetails(),
        eventSink = { actualValidationEvents.add(it) }
      )
    }.test {
      assertThat(awaitItem()).isEqualTo(FillingsOrderStep.State.Loading)

      awaitItem()
        .isInstanceOf<FillingsOrderStep.State.AvailableFillings>()
        .run {
          assertThat(selected).isNull()
          assertThat(list).isEqualTo(expected)
        }
    }

    assertThat(actualValidationEvents).isEqualTo(listOf(OrderStep.Validation.Invalid))
  }

  @Test
  fun `invoke - emit Valid validation event after selecting filling`() = runTest {
    val actualEvents = mutableListOf<OrderStep.Event>()
    val pear = Ingredient("pear")
    val expected = persistentListOf(
      Ingredient("apple"),
      Ingredient("orange"),
      pear,
    )
    val testRepo = TestRepo(expected)
    val producer = FillingsProducerImpl(testRepo)

    moleculeFlow(RecompositionClock.Immediate) {
      producer(
        orderDetails = OrderDetails(),
        eventSink = { actualEvents.add(it) }
      )
    }.test {
      awaitItem() // Loading

      // select a filling
      awaitItem()
        .isInstanceOf<FillingsOrderStep.State.AvailableFillings>()
        .run { eventSink(FillingsOrderStep.Event.SelectFilling(pear)) }

      // verify filling is selected
      awaitItem()
        .isInstanceOf<FillingsOrderStep.State.AvailableFillings>()
        .run {
          assertThat(selected).isEqualTo(pear)
        }
    }

    val expectedEvents = listOf(
      OrderStep.Validation.Invalid,
      OrderStep.UpdateOrder.Filling(pear),
      OrderStep.Validation.Valid
    )
    assertThat(actualEvents).isEqualTo(expectedEvents)
  }
}

private inline fun <reified T : Any> FillingsOrderStep.State.isInstanceOf(): T =
  (this as? T) ?: error("unable to cast $this to ${T::class.simpleName}")

private class TestRepo(private val fillings: ImmutableList<Ingredient>) : IngredientsRepository {
  override suspend fun getFillings(): ImmutableList<Ingredient> = fillings
  override suspend fun getToppings(): ImmutableList<Ingredient> = TODO("Not yet implemented")
}
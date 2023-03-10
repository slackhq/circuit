package com.slack.circuit.tacos.step

import androidx.compose.runtime.Composable
import com.slack.circuit.tacos.model.Ingredient
import com.slack.circuit.tacos.OrderDetails
import kotlinx.collections.immutable.ImmutableSet

sealed interface OrderStep {
  val number: Int

  sealed interface State // TODO CircuitUiState?

  sealed interface Event // TODO CircuitUiEvent?
  sealed interface Validation : Event {
    val enabled: Boolean
    object Invalid : Validation { override val enabled = false }
    object Valid : Validation { override val enabled = true }
  }

  sealed interface UpdateOrder : Event {
    data class Filling(val ingredient: Ingredient) : UpdateOrder
    data class Toppings(val ingredients: ImmutableSet<Ingredient>) : UpdateOrder
  }

  interface StateProducer<T : State> {
    @Composable
    operator fun invoke(orderDetails: OrderDetails, eventSink: (Event) -> Unit): T
  }
}

fun interface FillingsProducer : OrderStep.StateProducer<OrderStep.State>

fun interface ToppingsProducer : OrderStep.StateProducer<OrderStep.State>

fun interface SummaryProducer : OrderStep.StateProducer<OrderStep.State>

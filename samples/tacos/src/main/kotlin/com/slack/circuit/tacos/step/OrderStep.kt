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
    val ingredients: ImmutableSet<Ingredient>
    data class Fillings(override val ingredients: ImmutableSet<Ingredient>) : UpdateOrder
    data class Toppings(override val ingredients: ImmutableSet<Ingredient>) : UpdateOrder
  }

  interface StateProducer<T : State> {
    @Composable
    operator fun invoke(orderDetails: OrderDetails, eventSink: (Event) -> Unit): T
  }
}

interface FillingsProducer : OrderStep.StateProducer<OrderStep.State>

interface ToppingsProducer : OrderStep.StateProducer<OrderStep.State>

interface SummaryProducer : OrderStep.StateProducer<OrderStep.State>

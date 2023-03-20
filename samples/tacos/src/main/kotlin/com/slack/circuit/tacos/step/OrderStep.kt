// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient

sealed interface OrderStep {
  val index: Int
  @get:StringRes val headerResId: Int

  sealed interface State

  sealed interface Event
  sealed interface Validation : Event {
    val isOrderValid: Boolean
    object Invalid : Validation {
      override val isOrderValid = false
    }
    object Valid : Validation {
      override val isOrderValid = true
    }
  }

  sealed interface UpdateOrder : Event {
    data class SetFilling(val ingredient: Ingredient) : UpdateOrder
    data class SetToppings(val ingredients: Set<Ingredient>) : UpdateOrder
  }

  object Restart : Event

  interface StateProducer<T : State> {
    @Composable operator fun invoke(orderDetails: OrderDetails, eventSink: (Event) -> Unit): T
  }
}

fun interface FillingsProducer : OrderStep.StateProducer<OrderStep.State>

fun interface ToppingsProducer : OrderStep.StateProducer<OrderStep.State>

fun interface ConfirmationProducer : OrderStep.StateProducer<OrderStep.State>

fun interface SummaryProducer : OrderStep.StateProducer<OrderStep.State>

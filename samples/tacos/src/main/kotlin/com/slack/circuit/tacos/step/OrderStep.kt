// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tacos.step

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.slack.circuit.tacos.OrderDetails
import com.slack.circuit.tacos.model.Ingredient

sealed interface OrderStep {
  // used to simplify navigating between order steps
  val index: Int

  // displayed in wizard header
  @get:StringRes val headerResId: Int

  sealed interface State

  sealed interface Event

  /** Validation events affecting the users ability to advance to the next order step */
  sealed interface Validation : Event {
    val isOrderValid: Boolean

    // Order is in an invalid state. Do not allow user to proceed to next step
    object Invalid : Validation {
      override val isOrderValid = false
    }

    // Order is in a valid state. Allow user to proceed to next step
    object Valid : Validation {
      override val isOrderValid = true
    }
  }

  /** Update events impacting the global order state */
  sealed interface UpdateOrder : Event {
    // Update the current order with the provided filling
    data class SetFilling(val ingredient: Ingredient) : UpdateOrder

    // Update the current order with the provided toppings
    data class SetToppings(val ingredients: Set<Ingredient>) : UpdateOrder
  }

  // Reset order state and restart the order process
  object Restart : Event

  /** The primary [Composable] entry point to produce order step state */
  fun interface StateProducer<T : State> {
    /**
     * Implementing classes should consume [OrderDetails] and emit state extending
     * [OrderStep.State]. User selections impacting the overall order state should be emitted to the
     * parent circuit as [events][OrderStep.Event] using [eventSink].
     *
     * @param orderDetails current order state (from the parent circuit)
     * @param eventSink pipeline accepting [OrderStep] [events][OrderStep.Event]
     */
    @Composable operator fun invoke(orderDetails: OrderDetails, eventSink: (Event) -> Unit): T
  }
}

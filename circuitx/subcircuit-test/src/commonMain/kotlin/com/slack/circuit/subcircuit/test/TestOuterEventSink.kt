// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.test

import app.cash.turbine.Turbine
import com.slack.circuit.subcircuit.SubCircuitOuterEvent

/**
 * A Turbine-based test event sink that records outer events from a SubCircuit presenter and allows
 * making assertions about them.
 *
 * Example usage:
 * ```kotlin
 * @Test
 * fun outerEventsAreEmittedCorrectly() = runTest {
 *   val presenter = MySubPresenter()
 *   presenter.test {
 *     val state = awaitItem()
 *
 *     // Trigger an action that should emit an outer event
 *     state.eventSink(MyEvent.NavigateButtonClicked)
 *
 *     // Await and assert on the outer event
 *     val event = outerEvents.awaitEvent()
 *     assertThat(event).isEqualTo(MyOuterEvent.Navigate("details"))
 *   }
 * }
 * ```
 *
 * @see SubCircuitOuterEvent
 */
public class TestOuterEventSink<OuterEvent : SubCircuitOuterEvent> {

  private val turbine = Turbine<OuterEvent>()

  public operator fun invoke(event: OuterEvent) {
    turbine.add(event)
  }

  /** Awaits the next outer event emitted by the presenter. */
  public suspend fun awaitEvent(): OuterEvent = turbine.awaitItem()

  /** Asserts that no outer events have been emitted. */
  public fun assertNoEvents() {
    turbine.expectNoEvents()
  }

  /** Cancels the underlying turbine and asserts no unconsumed events remain. */
  public suspend fun cancelAndAssertEmpty() {
    turbine.cancel()
  }
}

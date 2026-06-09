// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.test

import androidx.compose.runtime.SnapshotMutationPolicy
import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState

/**
 * A SubCircuit-specific extension to [ReceiveTurbine] with extra helper functions for SubCircuit
 * testing.
 *
 * This implementation of [ReceiveTurbine] slightly alters the behavior of [awaitItem] by only
 * emitting items that are _different_ from the previously emitted item. It also provides access to
 * outer events emitted by the SubPresenter through the [outerEvents] property.
 */
public interface SubCircuitReceiveTurbine<
  OuterEvent : SubCircuitOuterEvent,
  State : SubCircuitUiState,
> : ReceiveTurbine<State> {

  /**
   * The [TestOuterEventSink] that collects outer events emitted by the SubPresenter. Use this to
   * make assertions about outer events.
   */
  public val outerEvents: TestOuterEventSink<OuterEvent>

  /**
   * Awaits the next item and asserts that it is _unchanged_ from the previous emission. Essentially
   * this is a sort of escape-hatch from the altered "distinct until changed" behavior of
   * [awaitItem] in this implementation and can be used to more or less assert no change in state
   * after the next recomposition.
   */
  public suspend fun awaitUnchanged()
}

internal class SubCircuitReceiveTurbineImpl<
  OuterEvent : SubCircuitOuterEvent,
  State : SubCircuitUiState,
>(
  private val delegate: ReceiveTurbine<State>,
  override val outerEvents: TestOuterEventSink<OuterEvent>,
  private val policy: SnapshotMutationPolicy<State>,
) : SubCircuitReceiveTurbine<OuterEvent, State>, ReceiveTurbine<State> by delegate {

  private var lastItem: State? = null

  override suspend fun awaitUnchanged() {
    val next = delegate.awaitItem()
    if (next != lastItem) {
      throw AssertionError("Expected unchanged item but received $next. Previous was $lastItem.")
    }
  }

  override suspend fun awaitItem(): State {
    while (true) {
      val last = lastItem
      val next = delegate.awaitItem()
      lastItem = next
      if (last == null) {
        return next
      } else if (!policy.equivalent(last, next)) {
        return next
      }
    }
  }
}

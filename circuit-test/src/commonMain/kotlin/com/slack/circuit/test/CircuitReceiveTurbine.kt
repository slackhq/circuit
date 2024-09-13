// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.runtime.CircuitUiState

/**
 * A Circuit-specific extension to [ReceiveTurbine] with extra helper functions for Circuit testing.
 *
 * This implementation of [ReceiveTurbine] slightly alters the behavior of [awaitItem] by only
 * emitting items that are _different_ from the previously emitted item.
 */
public interface CircuitReceiveTurbine<UiState : CircuitUiState> : ReceiveTurbine<UiState> {
  /**
   * Awaits the next item and asserts that it is _unchanged_ from the previous emission. Essentially
   * this is a sort of escape-hatch from the altered "distinct until changed" behavior of
   * [awaitItem] in this implementation.
   */
  public suspend fun awaitUnchanged()
}

internal fun <UiState : CircuitUiState> ReceiveTurbine<UiState>.asCircuitReceiveTurbine():
  CircuitReceiveTurbine<UiState> {
  return CircuitReceiveTurbineImpl(this)
}

private class CircuitReceiveTurbineImpl<UiState : CircuitUiState>(
  private val delegate: ReceiveTurbine<UiState>
) : CircuitReceiveTurbine<UiState>, ReceiveTurbine<UiState> by delegate {

  private var lastItem: UiState? = null

  override suspend fun awaitUnchanged() {
    val next = delegate.awaitItem()
    if (next != lastItem) {
      throw AssertionError(
        "Expected unchanged item but received ${next}. Previous was ${lastItem}."
      )
    }
  }

  override suspend fun awaitItem(): UiState {
    while (true) {
      val last = lastItem
      val next = delegate.awaitItem()
      lastItem = next
      if (next != last) {
        return next
      }
    }
  }
}

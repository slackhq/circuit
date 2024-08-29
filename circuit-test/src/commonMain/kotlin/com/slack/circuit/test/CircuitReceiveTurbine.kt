// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.runtime.CircuitUiState

/**
 * A Circuit-specific extension to [ReceiveTurbine] with extra helper functions for Circuit testing.
 */
public interface CircuitReceiveTurbine<UiState : CircuitUiState> : ReceiveTurbine<UiState> {
  /** Awaits the _next_ [UiState] emission that is different than the [lastState]. */
  public suspend fun consumeState(lastState: UiState? = tryMostRecentItem()): UiState
}

/**
 * Awaits a subtype of [UiState] of type [SubState]. This is useful for sealed state type
 * hierarchies.
 *
 * ```kotlin
 * presenter.test {
 *   val loadingState = consumeState<Loading>()
 *   // By default, will drop any unmatched intermediate emissions
 *   val loadedState = consumeState<Loaded>()
 *   // Optionally, disallow dropping to expect the next emission to be the expected type
 *   val loadedState = consumeState<Loaded>(dropUnmatchedIntermediates = false)
 * }
 * ```
 */
public suspend inline fun <reified SubState, UiState> CircuitReceiveTurbine<UiState>.consumeState(
  dropUnmatchedIntermediates: Boolean = true
): UiState where SubState : UiState, UiState : CircuitUiState {
  return awaitNext(dropUnmatchedIntermediates) { it is SubState }
}

internal fun <UiState : CircuitUiState> ReceiveTurbine<UiState>.tryMostRecentItem(): UiState? {
  return try {
    expectMostRecentItem()
  } catch (e: AssertionError) {
    null
  }
}

internal fun <UiState : CircuitUiState> ReceiveTurbine<UiState>.asCircuitReceiveTurbine():
  CircuitReceiveTurbine<UiState> {
  return CircuitReceiveTurbineImpl(this)
}

private class CircuitReceiveTurbineImpl<UiState : CircuitUiState>(
  private val delegate: ReceiveTurbine<UiState>
) : CircuitReceiveTurbine<UiState>, ReceiveTurbine<UiState> by delegate {
  override suspend fun consumeState(lastState: UiState?): UiState {
    return awaitNext(dropUnmatchedIntermediates = false) { value -> value != lastState }
  }
}

@PublishedApi
internal suspend fun <UiState : CircuitUiState> CircuitReceiveTurbine<UiState>.awaitNext(
  dropUnmatchedIntermediates: Boolean = true,
  predicate: (UiState) -> Boolean,
): UiState {
  var dropped = false
  while (true) {
    when (val event = awaitEvent()) {
      Event.Complete -> throw AssertionError("No new states emitted!")
      is Event.Error -> throw event.throwable
      is Event.Item -> {
        val value = event.value
        if (predicate(value)) {
          return value
        } else if (dropUnmatchedIntermediates) {
          continue
        } else if (dropped) {
          throw AssertionError("Dropped intermediate emission '$value' of type '${value::class}'.")
        } else {
          dropped = true
        }
      }
    }
  }
}

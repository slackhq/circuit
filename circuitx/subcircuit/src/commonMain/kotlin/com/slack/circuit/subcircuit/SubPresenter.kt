// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Presenter for SubCircuits that delegates events to an outer component.
 *
 * Unlike Circuit's Presenter, SubPresenter does not support navigation directly. Events that cannot
 * be handled within the SubCircuit (such as navigation) should be sent to the outer event sink
 * provided to [present].
 *
 * @param OuterEvent The type of events that will be sent to the outer event sink. Should implement
 *   [SubCircuitOuterEvent].
 * @param State The type of UI state produced by this presenter. Should implement
 *   [SubCircuitUiState].
 * @see SubScreen
 * @see SubCircuitContent
 * @see SubCircuitOuterEvent
 * @see SubCircuitUiState
 */
@Stable
public interface SubPresenter<OuterEvent : SubCircuitOuterEvent, State : SubCircuitUiState> {
  /**
   * Presents the UI state for this SubCircuit.
   *
   * @param outerEventSink A callback function for delegating events to the outer Circuit or
   *   Composable. Events that cannot be handled within the SubCircuit (such as navigation) should
   *   be sent to this sink.
   * @return The current UI state to be rendered.
   */
  @Composable public fun present(outerEventSink: (OuterEvent) -> Unit): State
}

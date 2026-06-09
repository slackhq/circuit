// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

/**
 * Marker interface for event types used in SubCircuit.
 *
 * Events represent user interactions or other triggers that should be handled by a presenter. In
 * SubCircuit, events are typically delegated as a [SubCircuitOuterEvent] to an outer event sink
 * provided by the parent component.
 *
 * Example:
 * ```kotlin
 * sealed interface MyEvent : SubCircuitUiEvent {
 *   data class ItemClicked(val id: String) : MyEvent
 *   data object RefreshRequested : MyEvent
 * }
 * ```
 *
 * @see SubPresenter
 * @see SubCircuitUiState
 */
public interface SubCircuitUiEvent

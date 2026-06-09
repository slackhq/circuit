// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import androidx.compose.runtime.Stable

/**
 * Marker interface for state types used in SubCircuit presenters.
 *
 * All state types returned from [SubPresenter.present] should implement this interface.
 *
 * Example:
 * ```kotlin
 * data class MyState(
 *   val title: String,
 *   val items: ImmutableList<Item>,
 *   val eventSink: (MyEvent) -> Unit,
 * ) : SubCircuitUiState
 * ```
 *
 * @see SubPresenter
 * @see SubCircuitUiEvent
 */
@Stable public interface SubCircuitUiState

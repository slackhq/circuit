// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * UI component for SubCircuits.
 *
 * Renders the provided [State] to the screen. This is a functional interface that can be
 * implemented as a lambda.
 *
 * @param State The type of UI state this component renders. Should implement [SubCircuitUiState].
 * @see SubCircuitUiState
 */
@Stable
public fun interface SubUi<in State : SubCircuitUiState> {
  @Composable public fun Content(state: State, modifier: Modifier)
}

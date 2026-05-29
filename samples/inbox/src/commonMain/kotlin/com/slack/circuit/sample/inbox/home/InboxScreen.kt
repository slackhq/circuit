// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.home

import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.list.InboxListScreen

/**
 * Root screen for the inbox sample. Composes the [InboxListScreen] and [EmailDetailScreen]
 * children into a single adaptive surface.
 */
@Parcelize
data object InboxScreen : Screen {
  data class State(
    val listState: InboxListScreen.State,
    /** Non-null when an email is selected; the detail pane renders this state. */
    val detailState: EmailDetailScreen.State?,
    /** Mirrors [detailState]'s identity for list-highlighting purposes. */
    val selectedEmailId: String?,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    /**
     * Clears the current selection (and so the detail pane). Emitted by the UI when the user
     * presses back on a compact layout, for instance.
     */
    data object ClearSelection : Event
  }
}

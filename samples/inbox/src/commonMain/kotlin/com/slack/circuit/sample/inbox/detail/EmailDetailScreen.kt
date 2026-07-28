// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.detail

import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.inbox.data.Email
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class EmailDetailScreen(val emailId: String) : Screen {
  sealed interface State : CircuitUiState {
    data object Loading : State

    data object NotFound : State

    data class Loaded(val email: Email, val eventSink: (Event) -> Unit) : State
  }

  sealed interface Event : CircuitUiEvent {
    data object BackClicked : Event

    data object ToggleStar : Event

    /** Flip the email back to unread. Useful when the auto-mark-on-open wasn't wanted. */
    data object MarkUnread : Event

    data object Archive : Event
  }
}

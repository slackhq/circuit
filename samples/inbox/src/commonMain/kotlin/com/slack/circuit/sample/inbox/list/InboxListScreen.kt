// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.list

import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sample.inbox.data.Email
import com.slack.circuit.sample.inbox.data.EmailFolder

@Parcelize
data class InboxListScreen(val folder: EmailFolder = EmailFolder.Inbox) : Screen {
  data class State(
    val folder: EmailFolder,
    val folders: List<EmailFolder>,
    val emails: List<Email>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class EmailClicked(val emailId: String) : Event

    data class ToggleStar(val emailId: String) : Event

    data class FolderChanged(val folder: EmailFolder) : Event
  }
}

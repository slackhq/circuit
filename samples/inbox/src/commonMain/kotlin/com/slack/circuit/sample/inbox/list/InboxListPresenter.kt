// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.sample.inbox.data.EmailFolder
import com.slack.circuit.sample.inbox.data.EmailRepository
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * List presenter that works both as a standalone screen and as a child of the composite inbox.
 *
 * Email clicks always go through [navigator]. A standalone screen receives the real navigator,
 * while the composite provides one that converts detail navigation into selection.
 */
@AssistedInject
class InboxListPresenter(
  @Assisted private val screen: InboxListScreen,
  @Assisted private val navigator: Navigator,
  private val emailRepository: EmailRepository,
) : Presenter<InboxListScreen.State> {

  @Composable
  override fun present(): InboxListScreen.State {
    var folder by rememberRetained { mutableStateOf(screen.folder) }
    val emailsFlow = remember(folder) { emailRepository.observeEmails(folder) }
    val emails by emailsFlow.collectAsState(initial = emptyList())

    return InboxListScreen.State(folder = folder, folders = EmailFolder.entries, emails = emails) {
      event ->
      when (event) {
        is InboxListScreen.Event.EmailClicked -> navigator.goTo(EmailDetailScreen(event.emailId))
        is InboxListScreen.Event.ToggleStar -> emailRepository.toggleStar(event.emailId)
        is InboxListScreen.Event.FolderChanged -> folder = event.folder
      }
    }
  }

  @CircuitInject(InboxListScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(screen: InboxListScreen, navigator: Navigator): InboxListPresenter
  }
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.sample.inbox.data.Email
import com.slack.circuit.sample.inbox.data.EmailRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.collect

/**
 * Detail presenter that works both as a standalone screen and as a child of the composite inbox.
 *
 * Back and archive always call [Navigator.pop]. A standalone screen pops the back stack, while
 * the composite turns that pop into clearing the current selection.
 */
@AssistedInject
class EmailDetailPresenter(
  @Assisted private val screen: EmailDetailScreen,
  @Assisted private val navigator: Navigator,
  private val emailRepository: EmailRepository,
) : Presenter<EmailDetailScreen.State> {

  @Composable
  override fun present(): EmailDetailScreen.State {
    val email by
      produceRetainedState<EmailLoadState>(EmailLoadState.Loading, screen.emailId) {
        emailRepository.observeEmail(screen.emailId).collect { fetched ->
          value = if (fetched == null) EmailLoadState.NotFound else EmailLoadState.Loaded(fetched)
        }
      }

    // Mark read on first open. The user can flip it back via the MarkUnread event below.
    LaunchedEffect(screen.emailId) { emailRepository.markRead(screen.emailId) }

    return when (val current = email) {
      EmailLoadState.Loading -> EmailDetailScreen.State.Loading
      EmailLoadState.NotFound -> EmailDetailScreen.State.NotFound
      is EmailLoadState.Loaded ->
        EmailDetailScreen.State.Loaded(current.email) { event ->
          when (event) {
            EmailDetailScreen.Event.BackClicked -> navigator.pop()
            EmailDetailScreen.Event.ToggleStar -> emailRepository.toggleStar(screen.emailId)
            EmailDetailScreen.Event.MarkUnread -> emailRepository.markUnread(screen.emailId)
            EmailDetailScreen.Event.Archive -> {
              emailRepository.archive(screen.emailId)
              navigator.pop()
            }
          }
        }
    }
  }

  @CircuitInject(EmailDetailScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(screen: EmailDetailScreen, navigator: Navigator): EmailDetailPresenter
  }
}

private sealed interface EmailLoadState {
  data object Loading : EmailLoadState

  data object NotFound : EmailLoadState

  data class Loaded(val email: Email) : EmailLoadState
}

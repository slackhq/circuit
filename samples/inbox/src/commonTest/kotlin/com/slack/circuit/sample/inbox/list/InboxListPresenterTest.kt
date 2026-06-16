// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.list

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.slack.circuit.sample.inbox.data.EmailFolder
import com.slack.circuit.sample.inbox.data.EmailRepository
import com.slack.circuit.sample.inbox.data.testEmails
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class InboxListPresenterTest {

  @Test
  fun `emits emails for the initial folder`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = InboxListScreen(folder = EmailFolder.Inbox)
    val presenter = InboxListPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      val state = awaitPopulated()
      // All three test emails are in the Inbox location (email 3 is starred but still in Inbox).
      assertThat(state.emails.map { it.id }).containsExactly("3", "2", "1")
      assertThat(state.folder).isEqualTo(EmailFolder.Inbox)
    }
  }

  @Test
  fun `email click navigates to detail screen`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = InboxListScreen(folder = EmailFolder.Inbox)
    val navigator = FakeNavigator(screen)
    val presenter = InboxListPresenter(screen, navigator, repository)
    presenter.test {
      awaitPopulated().eventSink(InboxListScreen.Event.EmailClicked("1"))
      assertThat(navigator.awaitNextScreen()).isEqualTo(EmailDetailScreen("1"))
    }
  }

  @Test
  fun `toggle star keeps the email in its location and only flips the flag`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = InboxListScreen(folder = EmailFolder.Inbox)
    val presenter = InboxListPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      val initial = awaitPopulated()
      assertThat(initial.emails.first { it.id == "1" }.isStarred).isEqualTo(false)
      initial.eventSink(InboxListScreen.Event.ToggleStar("1"))
      val next = awaitItem()
      // Still in the Inbox tab — order/contents unchanged, just isStarred toggled.
      assertThat(next.emails.map { it.id }).containsExactly("3", "2", "1")
      assertThat(next.emails.first { it.id == "1" }.isStarred).isEqualTo(true)
    }
  }

  @Test
  fun `folder change emits new emails`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = InboxListScreen(folder = EmailFolder.Inbox)
    val presenter = InboxListPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      awaitPopulated().eventSink(InboxListScreen.Event.FolderChanged(EmailFolder.Starred))
      val starred = awaitMatching {
        it.folder == EmailFolder.Starred &&
          it.emails.isNotEmpty() &&
          it.emails.all { e -> e.isStarred }
      }
      assertThat(starred.emails.map { it.id }).containsExactly("3")
      assertThat(starred.emails.single().isStarred).isTrue()
    }
  }
}

private suspend fun CircuitReceiveTurbine<InboxListScreen.State>.awaitPopulated() = awaitMatching {
  it.emails.isNotEmpty()
}

private suspend inline fun CircuitReceiveTurbine<InboxListScreen.State>.awaitMatching(
  crossinline predicate: (InboxListScreen.State) -> Boolean
): InboxListScreen.State {
  while (true) {
    val state = awaitItem()
    if (predicate(state)) return state
  }
}

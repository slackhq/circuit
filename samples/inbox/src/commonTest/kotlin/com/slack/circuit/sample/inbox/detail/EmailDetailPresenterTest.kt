// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.detail

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.sample.inbox.data.EmailLocation
import com.slack.circuit.sample.inbox.data.EmailRepository
import com.slack.circuit.sample.inbox.data.testEmails
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class EmailDetailPresenterTest {

  @Test
  fun `loads email and marks it read`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = EmailDetailScreen("1")
    val presenter = EmailDetailPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      val loaded = awaitLoaded()
      assertThat(loaded.email.id).isEqualTo("1")
      // markRead is dispatched in a LaunchedEffect; either the first state we see is already
      // read, or a follow-up emission flips it. Tolerate both.
      val readState = if (loaded.email.isRead) loaded else awaitLoaded()
      assertThat(readState.email.isRead).isTrue()
    }
  }

  @Test
  fun `not found state when email id is unknown`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = EmailDetailScreen("missing")
    val presenter = EmailDetailPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      assertThat(awaitItem()).isEqualTo(EmailDetailScreen.State.Loading)
      assertThat(awaitItem()).isEqualTo(EmailDetailScreen.State.NotFound)
    }
  }

  @Test
  fun `toggle star flips starred state without moving the email`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = EmailDetailScreen("1")
    val presenter = EmailDetailPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      val loaded = awaitLoaded()
      assertThat(loaded.email.isStarred).isFalse()
      loaded.eventSink(EmailDetailScreen.Event.ToggleStar)
      val starred = awaitLoaded()
      assertThat(starred.email.isStarred).isTrue()
      assertThat(starred.email.location).isEqualTo(EmailLocation.Inbox)
    }
  }

  @Test
  fun `mark unread flips the read flag`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = EmailDetailScreen("1")
    val presenter = EmailDetailPresenter(screen, FakeNavigator(screen), repository)
    presenter.test {
      val loaded = awaitLoaded()
      val read = if (loaded.email.isRead) loaded else awaitLoaded()
      assertThat(read.email.isRead).isTrue()
      read.eventSink(EmailDetailScreen.Event.MarkUnread)
      val unread = awaitLoaded()
      assertThat(unread.email.isRead).isFalse()
    }
  }

  @Test
  fun `archive pops and moves email to archive`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = EmailDetailScreen("1")
    val navigator = FakeNavigator(screen)
    val presenter = EmailDetailPresenter(screen, navigator, repository)
    presenter.test {
      awaitLoaded().eventSink(EmailDetailScreen.Event.Archive)
      val archived = awaitLoaded()
      assertThat(archived.email.location).isEqualTo(EmailLocation.Archive)
      navigator.awaitPop()
    }
  }

  @Test
  fun `back click pops the back stack`() = runTest {
    val repository = EmailRepository(seed = testEmails())
    val screen = EmailDetailScreen("1")
    val navigator = FakeNavigator(screen)
    val presenter = EmailDetailPresenter(screen, navigator, repository)
    presenter.test {
      awaitLoaded().eventSink(EmailDetailScreen.Event.BackClicked)
      navigator.awaitPop()
    }
  }
}

private suspend fun CircuitReceiveTurbine<EmailDetailScreen.State>.awaitLoaded():
  EmailDetailScreen.State.Loaded {
  while (true) {
    val state = awaitItem()
    if (state is EmailDetailScreen.State.Loaded) return state
  }
}

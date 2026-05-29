// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.home

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.sample.inbox.data.EmailRepository
import com.slack.circuit.sample.inbox.data.testEmails
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.di.inboxTestGraph
import com.slack.circuit.sample.inbox.list.InboxListScreen
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.presenterTestOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/**
 * Composite tests — verify that the composite assembles real `InboxListPresenter` /
 * `EmailDetailPresenter` instances (via Metro-generated `@AssistedFactory` impls) and that
 * their would-be navigation events flow through the intercepting `Navigator` stubs into the
 * composite's `selectedEmailId` state.
 */
class InboxPresenterTest {

  @Test
  fun `initial state has no selection and no detail`() = runTest {
    val graph = inboxTestGraph(EmailRepository(seed = testEmails()))
    presenterTestOf({ InboxPresenter(graph.inboxListFactory, graph.emailDetailFactory) }) {
      val state = awaitListPopulated()
      assertThat(state.selectedEmailId).isNull()
      assertThat(state.detailState).isNull()
    }
  }

  @Test
  fun `clicking an email in the list populates the detail pane`() = runTest {
    val graph = inboxTestGraph(EmailRepository(seed = testEmails()))
    presenterTestOf({ InboxPresenter(graph.inboxListFactory, graph.emailDetailFactory) }) {
      awaitListPopulated().listState.eventSink(InboxListScreen.Event.EmailClicked("1"))
      val withDetail = awaitDetailLoaded()
      assertThat(withDetail.selectedEmailId).isEqualTo("1")
      assertThat(withDetail.detailState is EmailDetailScreen.State.Loaded).isTrue()
    }
  }

  @Test
  fun `ClearSelection event nulls out the detail pane`() = runTest {
    val graph = inboxTestGraph(EmailRepository(seed = testEmails()))
    presenterTestOf({ InboxPresenter(graph.inboxListFactory, graph.emailDetailFactory) }) {
      awaitListPopulated().listState.eventSink(InboxListScreen.Event.EmailClicked("1"))
      val withDetail = awaitDetailLoaded()
      assertThat(withDetail.detailState).isNotNull()

      withDetail.eventSink(InboxScreen.Event.ClearSelection)
      val cleared = awaitMatching { it.detailState == null }
      assertThat(cleared.selectedEmailId).isNull()
    }
  }

  @Test
  fun `detail back click clears selection via the intercepting navigator`() = runTest {
    val graph = inboxTestGraph(EmailRepository(seed = testEmails()))
    presenterTestOf({ InboxPresenter(graph.inboxListFactory, graph.emailDetailFactory) }) {
      awaitListPopulated().listState.eventSink(InboxListScreen.Event.EmailClicked("1"))
      val detail = awaitDetailLoaded().detailState as EmailDetailScreen.State.Loaded
      detail.eventSink(EmailDetailScreen.Event.BackClicked)
      val cleared = awaitMatching { it.detailState == null }
      assertThat(cleared.selectedEmailId).isNull()
    }
  }
}

private suspend fun CircuitReceiveTurbine<InboxScreen.State>.awaitListPopulated() =
  awaitMatching { it.listState.emails.isNotEmpty() }

private suspend fun CircuitReceiveTurbine<InboxScreen.State>.awaitDetailLoaded() =
  awaitMatching { it.detailState is EmailDetailScreen.State.Loaded }

private suspend inline fun CircuitReceiveTurbine<InboxScreen.State>.awaitMatching(
  crossinline predicate: (InboxScreen.State) -> Boolean
): InboxScreen.State {
  while (true) {
    val state = awaitItem()
    if (predicate(state)) return state
  }
}

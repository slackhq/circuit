// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox

import com.slack.circuit.runtime.screen.restoreScreen
import com.slack.circuit.sample.inbox.data.EmailFolder
import com.slack.circuit.sample.inbox.detail.EmailDetailScreen
import com.slack.circuit.sample.inbox.di.buildCircuitSaver
import com.slack.circuit.sample.inbox.home.InboxScreen
import com.slack.circuit.sample.inbox.list.InboxListScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InboxCircuitSaverTest {
  private val saver = buildCircuitSaver()

  @Test
  fun inboxScreenRoundTrip() {
    val saved = assertNotNull(saver.save(InboxScreen))

    assertEquals(InboxScreen, saver.restoreScreen<InboxScreen>(saved))
  }

  @Test
  fun inboxListScreenRoundTrip() {
    val screen = InboxListScreen(EmailFolder.Archive)
    val saved = assertNotNull(saver.save(screen))

    assertEquals(screen, saver.restoreScreen<InboxListScreen>(saved))
  }

  @Test
  fun emailDetailScreenRoundTrip() {
    val screen = EmailDetailScreen("message-id")
    val saved = assertNotNull(saver.save(screen))

    assertEquals(screen, saver.restoreScreen<EmailDetailScreen>(saved))
  }
}

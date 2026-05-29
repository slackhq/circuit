// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@SingleIn(AppScope::class)
class EmailRepository(seed: List<Email>) {

  // Tests use the primary constructor to supply a small deterministic seed.
  @Inject constructor() : this(generateSeedEmails())

  private val emails = MutableStateFlow(seed.associateBy { it.id })

  /**
   * Observes emails for [folder].
   *
   * [EmailFolder.Starred] is virtual, so it includes starred emails from every storage location.
   */
  fun observeEmails(folder: EmailFolder): Flow<List<Email>> = emails.map { map ->
    map.values
      .filter {
        when (folder) {
          EmailFolder.Inbox -> it.location == EmailLocation.Inbox
          EmailFolder.Starred -> it.isStarred
          EmailFolder.Archive -> it.location == EmailLocation.Archive
        }
      }
      .sortedByDescending { it.id }
  }

  fun observeEmail(id: String): Flow<Email?> = emails.map { it[id] }

  fun unreadCount(folder: EmailFolder): Flow<Int> =
    observeEmails(folder).map { list -> list.count { !it.isRead } }

  fun markRead(id: String) {
    update(id) { it.copy(isRead = true) }
  }

  fun markUnread(id: String) {
    update(id) { it.copy(isRead = false) }
  }

  fun toggleStar(id: String) {
    update(id) { it.copy(isStarred = !it.isStarred) }
  }

  fun archive(id: String) {
    update(id) { it.copy(location = EmailLocation.Archive) }
  }

  private inline fun update(id: String, transform: (Email) -> Email) {
    emails.update { current ->
      val existing = current[id] ?: return@update current
      current + (id to transform(existing))
    }
  }
}

private val senders =
  listOf(
    "Ali Connors" to listOf("ali@example.com"),
    "Trevor Hansen" to listOf("trevor@example.com"),
    "Allison Trabucco" to listOf("allison@example.com"),
    "Frank Hawkins" to listOf("frank@example.com"),
    "Britta Holt" to listOf("britta@example.com"),
    "Sandra Adams" to listOf("sandra@example.com"),
    "Jeff Holden" to listOf("jeff@example.com", "team@example.com"),
    "Google Express" to listOf("you@example.com"),
  )

private val subjectTemplates =
  listOf(
    "Re: %s",
    "Quick thought on %s",
    "Follow-up: %s",
    "Heads up about %s",
    "Lunch this week?",
    "Meeting re-sched",
    "Welcome to %s",
    "Your order has shipped",
  )

private val timestamps =
  listOf("3:00 PM", "1:42 PM", "11:17 AM", "Yesterday", "Mon", "Sun", "Apr 24", "Mar 12")

private fun generateSeedEmails(): List<Email> {
  // Deterministic generation so the sample looks identical on every launch.
  val out = mutableListOf<Email>()
  val total = 24
  repeat(total) { i ->
    val (sender, recipients) = senders[i % senders.size]
    val subjectStem = LOREM_TOPICS[i % LOREM_TOPICS.size]
    val template = subjectTemplates[i % subjectTemplates.size]
    val subject = if (template.contains("%s")) template.format(subjectStem) else template
    val snippet = loremIpsum(words = 14, startOffset = i * 3)
    val body = buildString {
      append(loremIpsum(words = 60, startOffset = i * 7))
      append("\n\n")
      append(loremIpsum(words = 80, startOffset = i * 11))
    }
    val location = if (i % 7 == 0) EmailLocation.Archive else EmailLocation.Inbox
    // Pre-star a few emails across both locations so the Starred tab is non-empty out of the gate.
    val isStarred = i % 5 == 0
    out +=
      Email(
        // Pad id so sortedByDescending gives a stable newest-first order.
        id = (1000 - i).toString(),
        sender = sender,
        subject = subject,
        snippet = snippet,
        body = body,
        timestamp = timestamps[i % timestamps.size],
        recipients = recipients,
        isRead = i % 3 != 0,
        isStarred = isStarred,
        location = location,
      )
  }
  return out
}

private val LOREM_TOPICS =
  listOf(
    "the proposal",
    "Q3 planning",
    "the design review",
    "the rollout",
    "the launch",
    "onboarding",
  )

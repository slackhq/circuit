// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.data

internal fun testEmails(): List<Email> =
  listOf(
    Email(
      id = "1",
      sender = "Ali Connors",
      subject = "Meeting re-sched",
      snippet = "Hey, going to be out tomorrow.",
      body = "Hey, going to be out tomorrow. Can we move to Thursday?",
      timestamp = "3:00 PM",
      recipients = listOf("you@example.com"),
      isRead = false,
      isStarred = false,
      location = EmailLocation.Inbox,
    ),
    Email(
      id = "2",
      sender = "Trevor Hansen",
      subject = "Lunch this week?",
      snippet = "Any chance you're free Wednesday?",
      body = "Any chance you're free Wednesday or Thursday for lunch?",
      timestamp = "1:42 PM",
      recipients = listOf("you@example.com"),
      isRead = true,
      isStarred = false,
      location = EmailLocation.Inbox,
    ),
    Email(
      id = "3",
      sender = "Allison Trabucco",
      subject = "Project update",
      snippet = "Q3 plan is mostly done.",
      body = "Q3 plan is mostly done, last review pass and we're good.",
      timestamp = "Yesterday",
      recipients = listOf("you@example.com"),
      isRead = false,
      isStarred = true,
      // Note: starred email still lives in Inbox; the Starred tab is a virtual view.
      location = EmailLocation.Inbox,
    ),
  )

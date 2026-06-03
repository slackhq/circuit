// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.data

import androidx.compose.runtime.Immutable

@Immutable
data class Email(
  val id: String,
  val sender: String,
  val subject: String,
  val snippet: String,
  val body: String,
  val timestamp: String,
  val recipients: List<String>,
  val isRead: Boolean,
  val isStarred: Boolean,
  val location: EmailLocation,
)

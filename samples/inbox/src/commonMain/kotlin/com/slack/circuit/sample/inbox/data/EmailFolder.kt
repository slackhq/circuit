// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.data

/**
 * Folder shown by the list UI.
 *
 * [Starred] is a virtual folder: it can include messages from both [Inbox] and [Archive].
 */
enum class EmailFolder(val displayName: String) {
  Inbox("Inbox"),
  Starred("Starred"),
  Archive("Archive"),
}

/** Storage location of an email. Starred state is tracked separately. */
enum class EmailLocation {
  Inbox,
  Archive,
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.data

/**
 * A UI-level "tab" the user can filter the list by. Distinct from [EmailLocation] (the actual
 * storage state of an email) — for example, [Starred] is a virtual view across both [Inbox] and
 * [Archive] locations.
 */
enum class EmailFolder(val displayName: String) {
  Inbox("Inbox"),
  Starred("Starred"),
  Archive("Archive"),
}

/** Storage location of an email. The [EmailFolder.Starred] tab is virtual and not represented here. */
enum class EmailLocation {
  Inbox,
  Archive,
}

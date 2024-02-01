// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial.common

import androidx.compose.runtime.Immutable

@Immutable
data class Email(
  val id: String,
  val subject: String,
  val body: String,
  val sender: String,
  val timestamp: String,
  val recipients: List<String>,
)

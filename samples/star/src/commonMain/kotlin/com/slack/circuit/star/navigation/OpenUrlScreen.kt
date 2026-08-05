// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.navigation

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import dev.zacsweers.metro.AppScope

@CircuitSerializable(AppScope::class)
expect class OpenUrlScreen : Screen {
  val url: String

  constructor(url: String)
}

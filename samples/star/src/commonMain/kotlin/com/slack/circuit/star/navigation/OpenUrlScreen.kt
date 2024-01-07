// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.navigation

import com.slack.circuit.runtime.screen.Screen

expect class OpenUrlScreen(url: String) : Screen {
  val url: String
}

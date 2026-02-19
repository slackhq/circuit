// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION_ERROR")

package com.slack.circuit.sample.counter.browser

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.slack.circuit.sample.counter.CounterApp
import com.slack.circuit.sample.counter.CounterScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  CanvasBasedWindow { CounterApp(CounterScreen) }
}

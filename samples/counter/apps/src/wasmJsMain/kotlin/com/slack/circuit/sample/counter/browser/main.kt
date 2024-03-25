// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION_ERROR")

package com.slack.circuit.sample.counter.browser

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.CounterUiFactory

data object BrowserCounterScreen : CounterScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val circuit: Circuit =
    Circuit.Builder()
      .addPresenterFactory(CounterPresenterFactory())
      .addUiFactory(CounterUiFactory())
      .build()
  CanvasBasedWindow { CircuitCompositionLocals(circuit) { CircuitContent(BrowserCounterScreen) } }
}

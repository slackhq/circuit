// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.browser

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.CounterUiFactory
import org.jetbrains.compose.web.renderComposable

data object BrowserCounterScreen : CounterScreen

fun main() {
  val circuit: Circuit =
    Circuit.Builder()
      .addPresenterFactory(CounterPresenterFactory())
      .addUiFactory(CounterUiFactory())
      .build()
  renderComposable(rootElementId = "root") {
    CircuitCompositionLocals(circuit) { CircuitContent(BrowserCounterScreen) }
  }
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.sample.inbox.di.InboxAppGraph
import com.slack.circuit.sample.inbox.home.InboxScreen

fun main() {
  val graph = InboxAppGraph.create()
  val circuitSaver = requireNotNull(graph.circuit.circuitSaver)
  application {
    val windowState = rememberWindowState(size = DpSize(1100.dp, 800.dp))
    Window(state = windowState, title = "Circuit Inbox", onCloseRequest = ::exitApplication) {
      MaterialTheme {
        val backStack = rememberSaveableBackStack(InboxScreen, circuitSaver)
        val navigator = rememberCircuitNavigator(backStack) { exitApplication() }
        CircuitCompositionLocals(graph.circuit) {
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }
  }
}

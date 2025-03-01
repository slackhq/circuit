// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.tutorial.intro

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.tutorial.common.EmailRepository

fun main() {
  val emailRepository = EmailRepository()
  val circuit: Circuit =
    Circuit.Builder()
      .addPresenterFactory(DetailPresenter.Factory(emailRepository))
      .addPresenterFactory(InboxPresenter.Factory(emailRepository))
      .addUi<InboxScreen, InboxScreen.State> { state, modifier -> Inbox(state, modifier) }
      .addUi<DetailScreen, DetailScreen.State> { state, modifier -> EmailDetail(state, modifier) }
      .build()
  application {
    Window(title = "Tutorial", onCloseRequest = ::exitApplication) {
      MaterialTheme {
        val backStack = rememberSaveableBackStack(InboxScreen)
        val navigator = rememberCircuitNavigator(backStack) { exitApplication() }
        CircuitCompositionLocals(circuit) {
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }
  }
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui

@Composable
fun CounterApp(
  screen: CounterScreen,
  circuit: Circuit = buildCircuit(),
  colorScheme: ColorScheme = MaterialTheme.colorScheme,
  onRootPop: () -> Unit = {},
) {
  MaterialTheme(colorScheme = colorScheme) {
    CircuitCompositionLocals(circuit) {
      val navStack = rememberSaveableNavStack(screen)
      val navigator =
        rememberCircuitNavigator(navStack = navStack, enableBackHandler = true, onRootPop = { onRootPop() })
      NavigableCircuitContent(
        navigator = navigator,
        navStack = navStack,
      )
    }
  }
}

fun buildCircuit(
  presenterFactory: Presenter.Factory = CounterPresenterFactory(),
  uiFactory: Ui.Factory = CounterUiFactory(),
): Circuit {
  return Circuit.Builder().addPresenterFactory(presenterFactory).addUiFactory(uiFactory).build()
}

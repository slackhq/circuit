// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit

class NavigableCircuitViewModelStateTestActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.ViewModel)

    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack { push(TestScreen.ScreenA) }
        val navigator =
          rememberCircuitNavigator(
            backStack = backStack,
            onRootPop = {}, // no-op for tests
          )
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
  }
}

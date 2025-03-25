// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.mosaic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sample.counter.CounterApp
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.buildCircuit
import kotlinx.coroutines.awaitCancellation

data object MosaicCounterScreen : CounterScreen

class CounterUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      is CounterScreen -> counterUi()
      else -> null
    }
  }
}

private fun counterUi() = ui<CounterScreen.State> { state, _ -> Counter(state) }

@Composable
private fun Counter(state: CounterScreen.State) {
  Column(Modifier.onCounterKeyEvent(state.eventSink)) {
    Text("Use arrow keys to increment or decrement the count. Press “q” to exit.")
    Text("Count: ${state.count}", color = if (state.count < 0) Color.Red else Color.Unspecified)
  }
}

internal suspend fun runCounterScreen() = runMosaic {
  val circuit = remember {
    buildCircuit(uiFactory = CounterUiFactory())
      .newBuilder()
      .setDefaultNavDecoration(NavigatorDefaults.EmptyDecoration)
      .build()
  }
  var exit by remember { mutableStateOf(false) }
  CounterApp(screen = MosaicCounterScreen, circuit = circuit) { exit = true }
  // Mosaic exits if no effects are running, so we use this to keep it alive until the app exits
  if (!exit) {
    LaunchedEffect(Unit) { awaitCancellation() }
  }
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitContext
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.sample.counter.CounterEvent
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.CounterState
import com.slack.circuit.ui

object DesktopCounterScreen : CounterScreen

@Composable
fun Counter(state: CounterState, modifier: Modifier = Modifier) {
  val color = if (state.count >= 0) Color.Unspecified else MaterialTheme.colors.error
  Box(modifier.fillMaxSize()) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = "Count: ${state.count}",
        style = MaterialTheme.typography.h3,
        color = color
      )
      Spacer(modifier = Modifier.height(16.dp))
      val sink = state.eventSink
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { sink(CounterEvent.Increment) }
      ) {
        Icon(rememberVectorPainter(Icons.Filled.Add), "Increment")
      }
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { sink(CounterEvent.Decrement) }
      ) {
        Icon(rememberVectorPainter(Remove), "Decrement")
      }
    }
  }
}

class CounterUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): ScreenUi? {
    return when (screen) {
      is CounterScreen -> ScreenUi(ui<CounterState> { state -> Counter(state) })
      else -> null
    }
  }
}

// TODO these don't appear to work in Android Studio :(
@Preview
@Composable
private fun CounterPreview() {
  Counter(CounterState(0))
}

@Preview
@Composable
private fun CounterPreviewNegative() {
  Counter(CounterState(-1))
}

fun main() =
  singleWindowApplication(
    title = "Counter Circuit (Desktop)",
    state = WindowState(width = 300.dp, height = 300.dp)
  ) {
    val circuitConfig: CircuitConfig =
      CircuitConfig.Builder()
        .addPresenterFactory(CounterPresenterFactory())
        .addUiFactory(CounterUiFactory())
        .build()
    MaterialTheme {
      CircuitCompositionLocals(circuitConfig) { CircuitContent(DesktopCounterScreen) }
    }
  }

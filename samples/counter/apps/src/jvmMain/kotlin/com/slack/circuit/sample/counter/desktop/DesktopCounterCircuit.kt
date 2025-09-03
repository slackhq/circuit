// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sample.counter.CounterScreen
import com.slack.circuit.sample.counter.PrimeScreen
import com.slack.circuit.sample.counter.Remove
import com.slack.circuit.sample.counter.buildCircuit

data object DesktopCounterScreen : CounterScreen

data class DesktopPrimeScreen(override val number: Int) : PrimeScreen

@Composable
fun Counter(state: CounterScreen.State, modifier: Modifier = Modifier) {
  val color = if (state.count >= 0) Color.Unspecified else MaterialTheme.colors.error
  Box(modifier.fillMaxSize()) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = "Count: ${state.count}",
        style = MaterialTheme.typography.h3,
        color = color,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row {
        Button(
          modifier = Modifier.padding(2.dp),
          onClick = { state.eventSink(CounterScreen.Event.Decrement) },
        ) {
          Icon(rememberVectorPainter(Remove), "Decrement")
        }
        Button(
          onClick = { state.eventSink(CounterScreen.Event.GoTo(DesktopPrimeScreen(state.count))) },
          modifier = Modifier.padding(2.dp),
        ) {
          Text(modifier = Modifier.padding(4.dp), text = "Prime?")
        }
        Button(
          modifier = Modifier.padding(2.dp),
          onClick = { state.eventSink(CounterScreen.Event.Increment) },
        ) {
          Icon(rememberVectorPainter(Icons.Filled.Add), "Increment")
        }
      }
    }
  }
}

@Composable
fun Prime(state: PrimeScreen.State, modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize()) {
    Column(Modifier.align(Alignment.Center)) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.h3,
        text = "${state.number}",
      )
      Spacer(modifier = Modifier.height(16.dp))
      if (state.isPrime) {
        Text(
          modifier = Modifier.align(Alignment.CenterHorizontally),
          text = "${state.number} is a prime number!",
        )
      } else {
        Text(
          modifier = Modifier.align(Alignment.CenterHorizontally),
          text = "${state.number} is not a prime number.",
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { state.eventSink(PrimeScreen.Event.Pop) },
      ) {
        Text("Back")
      }
    }
  }
}

class CounterUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      is CounterScreen -> ui<CounterScreen.State> { state, modifier -> Counter(state, modifier) }
      is PrimeScreen -> ui<PrimeScreen.State> { state, modifier -> Prime(state, modifier) }
      else -> null
    }
  }
}

// TODO these don't appear to work in Android Studio :(
@Preview
@Composable
private fun CounterPreview() {
  Counter(CounterScreen.State(0))
}

@Preview
@Composable
private fun CounterPreviewNegative() {
  Counter(CounterScreen.State(-1))
}

fun main() = application {
  Window(
    title = "Counter Circuit (Desktop)",
    state = WindowState(width = 300.dp, height = 300.dp),
    onCloseRequest = ::exitApplication,
  ) {
    val initialBackStack = listOf<Screen>(DesktopCounterScreen)
    val backStack = rememberSaveableBackStack(initialBackStack)
    val navigator = rememberCircuitNavigator(backStack) { exitApplication() }
    val circuit = remember { buildCircuit(uiFactory = CounterUiFactory()) }

    MaterialTheme {
      CircuitCompositionLocals(circuit) {
        NavigableCircuitContent(
          navigator = navigator,
          backStack = backStack,
          modifier = Modifier.backHandler(enabled = backStack.size > 1, onBack = navigator::pop),
        )
      }
    }
  }
}

@Composable
private fun Modifier.backHandler(enabled: Boolean, onBack: () -> Unit): Modifier {
  val focusRequester = remember { FocusRequester() }
  return focusRequester(focusRequester)
    .onPlaced { focusRequester.requestFocus() }
    .onPreviewKeyEvent {
      if (enabled && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
        onBack()
        true
      } else {
        false
      }
    }
}

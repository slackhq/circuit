// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter.mosaic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.MosaicScope
import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sample.counter.CounterPresenterFactory
import com.slack.circuit.sample.counter.CounterScreen
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jline.terminal.TerminalBuilder

data object MosaicCounterScreen : CounterScreen

class CounterUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return when (screen) {
      MosaicCounterScreen -> counterUi()
      else -> null
    }
  }
}

private fun counterUi(): Ui<CounterScreen.State> = ui { state, _ -> Counter(state) }

@Composable
private fun Counter(state: CounterScreen.State) {
  Column {
    Text("Use arrow keys to increment or decrement the count. Press “q” to exit.")
    Text("Count: ${state.count}")
  }

  // TODO THIS DOES NOT WORK! Needs https://github.com/JakeWharton/mosaic/issues/3
  //  This is left as a toe-hold for the future.
  LaunchedEffect(Unit) {
    withContext(IO) {
      val terminal = TerminalBuilder.terminal()
      terminal.enterRawMode()
      val reader = terminal.reader()

      @Suppress("MagicNumber")
      while (true) {
        // TODO https://github.com/JakeWharton/mosaic/issues/10
        when (reader.read()) {
          'q'.code -> {
            // TODO how do we quit? Pop + exit the parent mosaic exec?
            break
          }
          27 -> {
            when (reader.read()) {
              91 -> {
                when (reader.read()) {
                  65 -> state.eventSink(CounterScreen.Event.Increment)
                  66 -> state.eventSink(CounterScreen.Event.Decrement)
                }
              }
            }
          }
        }
      }
    }
  }
}

internal suspend fun runCounterScreen(useCircuit: Boolean) = runMosaic {
  if (useCircuit) {
    runCircuitCounterScreen()
  } else {
    runStandardCounterScreen()
  }
}

internal fun MosaicScope.runCircuitCounterScreen() {
  setContent {
    val circuit =
      Circuit.Builder()
        .addPresenterFactory(CounterPresenterFactory())
        .addUiFactory(CounterUiFactory())
        .build()
    CircuitCompositionLocals(circuit) { CircuitContent(MosaicCounterScreen) }
  }
}

// A working, circuit-less implementation for reference
private suspend fun MosaicScope.runStandardCounterScreen() {
  // TODO https://github.com/JakeWharton/mosaic/issues/3
  var count by mutableStateOf(0)

  setContent {
    Column {
      Text("Use arrow keys to increment or decrement the count. Press “q” to exit.")
      Text("Count: $count")
    }
  }

  withContext(IO) {
    val terminal = TerminalBuilder.terminal()
    terminal.enterRawMode()
    val reader = terminal.reader()

    @Suppress("MagicNumber")
    while (true) {
      // TODO https://github.com/JakeWharton/mosaic/issues/10
      when (reader.read()) {
        'q'.code -> break
        27 -> {
          when (reader.read()) {
            91 -> {
              when (reader.read()) {
                65 -> count++
                66 -> count--
              }
            }
          }
        }
      }
    }
  }
}

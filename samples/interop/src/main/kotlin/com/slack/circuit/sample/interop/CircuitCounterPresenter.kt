// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

class CircuitCounterPresenter : Presenter<CounterScreen.State> {
  @Composable
  override fun present(): CounterScreen.State {
    var count by rememberRetained { mutableIntStateOf(0) }

    return CounterScreen.State(count) { event ->
      when (event) {
        is CounterScreen.Event.Increment -> count++
        is CounterScreen.Event.Decrement -> count--
      }
    }
  }
}

@Parcelize
data object CounterScreen : Screen {
  data class State(val count: Int, val eventSink: (Event) -> Unit = {}) : CircuitUiState

  sealed interface Event : CircuitUiEvent {

    data object Increment : Event

    data object Decrement : Event
  }
}

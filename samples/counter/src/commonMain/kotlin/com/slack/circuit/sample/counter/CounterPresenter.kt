// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.runtime.Screen
import com.slack.circuit.presenterOf

data class CounterState(
  val count: Int,
  val eventSink: (CounterEvent) -> Unit = {},
) : CircuitUiState

sealed interface CounterEvent : CircuitUiEvent {
  object Increment : CounterEvent
  object Decrement : CounterEvent
}

@Composable
fun CounterPresenter(): CounterState {
  var count by remember { mutableStateOf(0) }

  return CounterState(count) { event ->
    when (event) {
      is CounterEvent.Increment -> count++
      is CounterEvent.Decrement -> count--
    }
  }
}

// Unfortunately can't make this multiplatform by itself because plugin.parcelize doesn't play nice
// in multiplatform android library projects
interface CounterScreen : Screen

class CounterPresenterFactory : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? {
    return when (screen) {
      is CounterScreen -> presenterOf { CounterPresenter() }
      else -> null
    }
  }
}

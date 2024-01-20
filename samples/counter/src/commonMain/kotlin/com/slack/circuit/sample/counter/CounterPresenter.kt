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
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen

sealed interface CounterEvent : CircuitUiEvent {
  data object Increment : CounterEvent

  data object Decrement : CounterEvent
}

@Composable
fun CounterPresenter(navigator: Navigator): CounterScreen.State {
  var count by remember { mutableStateOf(0) }

  return CounterScreen.State(count) { event ->
    when (event) {
      is CounterScreen.Event.GoTo -> navigator.goTo(event.screen)
      is CounterScreen.Event.Increment -> count++
      is CounterScreen.Event.Decrement -> count--
    }
  }
}

@Composable
fun PrimePresenter(navigator: Navigator, number: Int): PrimeScreen.State {
  return PrimeScreen.State(number, isPrime(number)) { event ->
    if (event is PrimeScreen.Event.Pop) navigator.pop()
  }
}

private fun isPrime(value: Int): Boolean {
  return (2..value / 2).none { value % it == 0 }
}

// Unfortunately can't make this multiplatform by itself because plugin.parcelize doesn't play nice
// in multiplatform android library projects
interface CounterScreen : Screen {
  data class State(val count: Int, val eventSink: (Event) -> Unit = {}) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class GoTo(val screen: Screen) : Event

    data object Increment : Event

    data object Decrement : Event
  }
}

interface PrimeScreen : Screen {
  data class State(val number: Int, val isPrime: Boolean, val eventSink: (Event) -> Unit = {}) :
    CircuitUiState

  sealed interface Event {
    data object Pop : Event
  }

  val number: Int
}

class CounterPresenterFactory : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? {
    return when (screen) {
      is CounterScreen -> presenterOf { CounterPresenter(navigator) }
      is PrimeScreen -> presenterOf { PrimePresenter(navigator, screen.number) }
      else -> null
    }
  }
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** A basic [Flow]-based presenter interface. */
fun interface FlowPresenter<UiState : Any, UiEvent : Any> {
  fun present(scope: CoroutineScope, events: Flow<UiEvent>): StateFlow<UiState>
}

/** An [Flow] presenter that exposes a [StateFlow] of count changes. */
class FlowCounterPresenter : FlowPresenter<Int, CounterScreen.Event> {
  private val count = MutableStateFlow(0)

  override fun present(scope: CoroutineScope, events: Flow<CounterScreen.Event>): StateFlow<Int> {
    scope.launch {
      events.collect {
        when (it) {
          is CounterScreen.Event.Increment -> {
            count.emit(count.value + 1)
          }
          is CounterScreen.Event.Decrement -> {
            count.emit(count.value - 1)
          }
        }
      }
    }
    return count
  }
}

/** Interop from a [FlowPresenter] to a Circuit [Presenter]. */
fun FlowPresenter<Int, CounterScreen.Event>.asCircuitPresenter(): Presenter<CounterScreen.State> {
  return presenterOf {
    val channel = remember { Channel<CounterScreen.Event>(Channel.BUFFERED) }
    val eventsFlow = remember { channel.receiveAsFlow() }
    val scope = rememberCoroutineScope()
    val state by remember { present(scope, eventsFlow) }.collectAsState()
    CounterScreen.State(state, channel::trySend)
  }
}

/**
 * Interop from a Circuit [Presenter] to a [FlowPresenter].
 *
 * Nuance here is that this needs to know how to access the underlying event sink.
 */
fun Presenter<CounterScreen.State>.asFlowPresenter(): FlowPresenter<Int, CounterScreen.Event> {
  return FlowPresenter { scope, events ->
    scope.launchMolecule(RecompositionMode.Immediate) {
      val (count, eventSink) = present()
      LaunchedEffect(eventSink) { events.collect(eventSink) }
      count
    }
  }
}

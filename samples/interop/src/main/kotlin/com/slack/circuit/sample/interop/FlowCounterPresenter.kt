// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.interop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.sample.counter.CounterEvent
import com.slack.circuit.sample.counter.CounterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An [Flow] presenter that exposes a [StateFlow] of count changes. */
class FlowCounterPresenter {
  private val count = MutableStateFlow(0)

  fun increment() {
    count.tryEmit(count.value + 1)
  }

  fun decrement() {
    count.tryEmit(count.value - 1)
  }

  fun countStateFlow(): StateFlow<Int> = count
}

fun FlowCounterPresenter.asCircuitPresenter(): Presenter<CounterState> {
  return presenterOf {
    val count by countStateFlow().collectAsState()
    CounterState(count) { event ->
      when (event) {
        is CounterEvent.Increment -> increment()
        is CounterEvent.Decrement -> decrement()
      }
    }
  }
}

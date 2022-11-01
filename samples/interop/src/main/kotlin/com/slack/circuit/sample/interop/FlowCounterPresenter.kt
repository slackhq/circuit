/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.sample.interop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.slack.circuit.Presenter
import com.slack.circuit.presenterOf
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

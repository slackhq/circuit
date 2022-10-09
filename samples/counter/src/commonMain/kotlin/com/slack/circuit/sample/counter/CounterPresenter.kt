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
package com.slack.circuit.sample.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
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
    circuitConfig: CircuitConfig,
  ): Presenter<*>? {
    return when (screen) {
      is CounterScreen -> presenterOf { CounterPresenter() }
      else -> null
    }
  }
}

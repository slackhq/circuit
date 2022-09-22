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
package com.slack.circuit.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.CompositeCircuitUiEvent
import com.slack.circuit.Presenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

@Composable
inline fun <reified E : CompositeCircuitUiEvent, reified R> rememberFilterEventAndGetState(
  events: Flow<CircuitUiEvent>,
  presenter: Presenter<CircuitUiState, CircuitUiEvent>
): R {
  val rememberEventFlow = remember { events.filterIsInstance<E>().map { it.event } }
  return presenter.present(rememberEventFlow) as R
}

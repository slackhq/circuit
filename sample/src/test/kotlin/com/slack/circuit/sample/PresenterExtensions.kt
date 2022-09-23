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
package com.slack.circuit.sample

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

suspend fun <UiState : CircuitUiState, UiEvent : CircuitUiEvent> Presenter<UiState, UiEvent>.test(
  timeout: Duration? = null,
  block: suspend ReceiveTurbine<UiState>.() -> Unit
) = test(emptyFlow(), timeout, block)

suspend fun <UiState : CircuitUiState, UiEvent : CircuitUiEvent> Presenter<UiState, UiEvent>.test(
  events: Flow<UiEvent>,
  timeout: Duration? = null,
  block: suspend ReceiveTurbine<UiState>.() -> Unit
) {
  moleculeFlow(RecompositionClock.Immediate) { present(events) }.test(timeout, block)
}

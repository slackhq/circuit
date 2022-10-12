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
package com.slack.circuit.test

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import kotlin.time.Duration

/**
 * Presents this [Presenter] and invokes a `suspend` [ReceiveTurbine] [block] that can be used to
 * assert state emissions from this presenter.
 *
 * @see moleculeFlow
 * @see test
 *
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param block the block to invoke.
 */
public suspend fun <UiState : CircuitUiState> Presenter<UiState>.test(
  timeout: Duration? = null,
  name: String? = null,
  block: suspend ReceiveTurbine<UiState>.() -> Unit
) {
  presenterTestOf({ present() }, timeout, name, block)
}

/**
 * Presents this [presentFunction] and invokes a `suspend` [ReceiveTurbine] [block] that can be used
 * to assert state emissions from it.
 *
 * @see moleculeFlow
 * @see test
 *
 * @param presentFunction the [Composable] present function being tested.
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param block the block to invoke.
 */
public suspend fun <UiState : CircuitUiState> presenterTestOf(
  presentFunction: @Composable () -> UiState,
  timeout: Duration? = null,
  name: String? = null,
  block: suspend ReceiveTurbine<UiState>.() -> Unit
) {
  moleculeFlow(RecompositionClock.Immediate, presentFunction).test(timeout, name, block)
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Presents this [Presenter] and invokes a `suspend` [ReceiveTurbine] [block] that can be used to
 * assert state emissions from this presenter.
 *
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param moleculeFlowTransformer an optional transformer for the underlying [moleculeFlow]. Must
 *   still return a [Flow] of type [UiState], but can be used for custom filtering. By default, it
 *   runs [distinctUntilChanged].
 * @param block the block to invoke.
 * @see moleculeFlow
 * @see test
 */
public suspend fun <UiState : CircuitUiState> Presenter<UiState>.test(
  timeout: Duration? = null,
  name: String? = null,
  moleculeFlowTransformer: (Flow<UiState>) -> Flow<UiState> = Flow<UiState>::distinctUntilChanged,
  block: suspend ReceiveTurbine<UiState>.() -> Unit,
) {
  presenterTestOf({ present() }, timeout, name, moleculeFlowTransformer, block)
}

/**
 * Presents this [presentFunction] and invokes a `suspend` [ReceiveTurbine] [block] that can be used
 * to assert state emissions from it.
 *
 * @param presentFunction the [Composable] present function being tested.
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param moleculeFlowTransformer an optional transformer for the underlying [moleculeFlow]. Must
 *   still return a [Flow] of type [UiState], but can be used for custom filtering. By default, it
 *   runs [distinctUntilChanged].
 * @param block the block to invoke.
 * @see moleculeFlow
 * @see test
 */
public suspend fun <UiState : CircuitUiState> presenterTestOf(
  presentFunction: @Composable () -> UiState,
  timeout: Duration? = null,
  name: String? = null,
  moleculeFlowTransformer: (Flow<UiState>) -> Flow<UiState> = Flow<UiState>::distinctUntilChanged,
  block: suspend ReceiveTurbine<UiState>.() -> Unit,
) {
  moleculeFlow(RecompositionMode.Immediate, presentFunction)
    .run(moleculeFlowTransformer)
    .test(timeout, name, block)
}

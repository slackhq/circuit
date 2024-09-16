// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.structuralEqualityPolicy
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.slack.circuit.foundation.internal.withCompositionLocalProvider
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration

/**
 * Presents this [Presenter] and invokes a `suspend` [ReceiveTurbine] [block] that can be used to
 * assert state emissions from this presenter.
 *
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param policy a policy to controls how state changes are compared in
 *   [CircuitReceiveTurbine.awaitItem].
 * @param block the block to invoke.
 * @see moleculeFlow
 * @see test
 */
public suspend fun <UiState : CircuitUiState> Presenter<UiState>.test(
  timeout: Duration? = null,
  name: String? = null,
  policy: SnapshotMutationPolicy<UiState> = structuralEqualityPolicy(),
  retainedStateRegistry: RetainedStateRegistry = RetainedStateRegistry(),
  block: suspend CircuitReceiveTurbine<UiState>.() -> Unit,
) {
  presenterTestOf({ present() }, timeout, name, policy, retainedStateRegistry, block)
}

/**
 * Presents this [presentFunction] and invokes a `suspend` [CircuitReceiveTurbine] [block] that can
 * be used to assert state emissions from it.
 *
 * @param presentFunction the [Composable] present function being tested.
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param policy a policy to controls how state changes are compared in
 *   [CircuitReceiveTurbine.awaitItem].
 * @param retainedStateRegistry a [RetainedStateRegistry] that can operate
 * @param block the block to invoke.
 * @see moleculeFlow
 * @see test
 */
public suspend fun <UiState : CircuitUiState> presenterTestOf(
  presentFunction: @Composable () -> UiState,
  timeout: Duration? = null,
  name: String? = null,
  policy: SnapshotMutationPolicy<UiState> = structuralEqualityPolicy(),
  retainedStateRegistry: RetainedStateRegistry = RetainedStateRegistry(),
  block: suspend CircuitReceiveTurbine<UiState>.() -> Unit,
) {
  try {
    moleculeFlow(RecompositionMode.Immediate, decorate(presentFunction, retainedStateRegistry))
      .test(timeout, name) { asCircuitReceiveTurbine(policy).block() }
  } finally {
    retainedStateRegistry.forgetUnclaimedValues()
  }
}

@OptIn(InternalCircuitApi::class)
private fun <UiState : CircuitUiState> decorate(
  presentFunction: @Composable () -> UiState,
  retainedStateRegistry: RetainedStateRegistry,
): @Composable () -> UiState {
  val newFunction =
    @Composable {
      withCompositionLocalProvider(LocalRetainedStateRegistry provides retainedStateRegistry) {
        presentFunction()
      }
    }
  return newFunction
}

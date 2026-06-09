// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.structuralEqualityPolicy
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubPresenter
import kotlin.time.Duration

/**
 * Presents this [SubPresenter] and invokes a `suspend` [SubCircuitReceiveTurbine] [block] that can
 * be used to assert state emissions from this presenter.
 *
 * Example usage:
 * ```kotlin
 * @Test
 * fun myTest() = runTest {
 *   val presenter = MySubPresenter()
 *   presenter.test {
 *     val state = awaitItem()
 *     assertEquals("Expected Title", state.title)
 *
 *     state.eventSink(MyEvent.Click)
 *     val updatedState = awaitItem()
 *     assertEquals(MyOuterEvent.NavigateToDetails, outerEvents.awaitEvent())
 *   }
 * }
 * ```
 *
 * @param outerEventSink an optional sink for outer events. Defaults to a [TestOuterEventSink] that
 *   collects all outer events for later assertion.
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param name an optional name for the test (used in error messages).
 * @param policy a policy to control how state changes are compared in
 *   [SubCircuitReceiveTurbine.awaitItem].
 * @param block the block to invoke with the test turbine scope.
 * @see moleculeFlow
 * @see test
 */
public suspend fun <OuterEvent : SubCircuitOuterEvent, State : SubCircuitUiState> SubPresenter<
  OuterEvent,
  State,
>
  .test(
  outerEventSink: TestOuterEventSink<OuterEvent> = TestOuterEventSink(),
  timeout: Duration? = null,
  name: String? = null,
  policy: SnapshotMutationPolicy<State> = structuralEqualityPolicy(),
  block: suspend SubCircuitReceiveTurbine<OuterEvent, State>.() -> Unit,
) {
  subPresenterTestOf(
    presentFunction = { present(outerEventSink::invoke) },
    outerEventSink = outerEventSink,
    timeout = timeout,
    name = name,
    policy = policy,
    block = block,
  )
}

/**
 * Presents this [presentFunction] and invokes a `suspend` [SubCircuitReceiveTurbine] [block] that
 * can be used to assert state emissions from it.
 *
 * This is useful when you need to provide additional composition locals or wrap the present
 * function.
 *
 * @param presentFunction the [Composable] present function being tested.
 * @param outerEventSink the sink for outer events that will be passed to the present function.
 * @param timeout an optional timeout for the test. Defaults to 1 second (in Turbine) if undefined.
 * @param name an optional name for the test (used in error messages).
 * @param policy a policy to control how state changes are compared in
 *   [SubCircuitReceiveTurbine.awaitItem].
 * @param block the block to invoke with the test turbine scope.
 * @see moleculeFlow
 * @see test
 */
public suspend fun <
  OuterEvent : SubCircuitOuterEvent,
  State : SubCircuitUiState,
> subPresenterTestOf(
  presentFunction: @Composable () -> State,
  outerEventSink: TestOuterEventSink<OuterEvent> = TestOuterEventSink(),
  timeout: Duration? = null,
  name: String? = null,
  policy: SnapshotMutationPolicy<State> = structuralEqualityPolicy(),
  block: suspend SubCircuitReceiveTurbine<OuterEvent, State>.() -> Unit,
) {
  moleculeFlow(RecompositionMode.Immediate, body = presentFunction).test(timeout, name) {
    asSubCircuitReceiveTurbine(outerEventSink, policy).block()
  }
}

internal fun <OuterEvent : SubCircuitOuterEvent, State : SubCircuitUiState> ReceiveTurbine<State>
  .asSubCircuitReceiveTurbine(
  outerEventSink: TestOuterEventSink<OuterEvent>,
  policy: SnapshotMutationPolicy<State>,
): SubCircuitReceiveTurbine<OuterEvent, State> {
  return SubCircuitReceiveTurbineImpl(this, outerEventSink, policy)
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Collects values from this [StateFlow] and represents its latest value via a retained [State]. The
 * [StateFlow.value] is used as an initial value. Every time there would be new value posted into
 * the [StateFlow] the returned [State] will be updated causing recomposition of every [State.value]
 * usage.
 *
 * @param context [CoroutineContext] to use for collecting.
 */
@Composable
public fun <T> StateFlow<T>.collectAsRetainedState(
  context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsRetainedState(value, context)

/**
 * Collects values from this [Flow] and represents its latest value via retained [State]. Every time
 * there would be new value posted into the [Flow] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * @param context [CoroutineContext] to use for collecting.
 */
@Composable
public fun <T : R, R> Flow<T>.collectAsRetainedState(
  initial: R,
  context: CoroutineContext = EmptyCoroutineContext,
): State<R> =
  produceRetainedState(initial, this, context) {
    if (context == EmptyCoroutineContext) {
      collect { value = it }
    } else withContext(context) { collect { value = it } }
  }

/**
 * Collects values from a [Flow] provided by the [flow] lambda and represents its latest value via
 * retained [State]. Every time there would be new value posted into the [Flow] the returned [State]
 * will be updated causing recomposition of every [State.value] usage.
 *
 * This function safely collects a Flow by retaining the [inputs] across configuration changes,
 * while the Flow itself is only remembered for the composition lifetime. This prevents memory leaks
 * from retaining the Flow instance.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [flow] to be rerun
 * @param initial the initial value for the state
 * @param context [CoroutineContext] to use for collecting.
 * @param flow a lambda that provides the [Flow] to collect from
 */
@Composable
public fun <T : R, R> collectAsRetainedState(
  vararg inputs: Any?,
  initial: R,
  context: CoroutineContext = EmptyCoroutineContext,
  flow: () -> Flow<T>,
): State<R> = rememberRetained(*inputs) { flow() }.collectAsRetainedState(initial, context)

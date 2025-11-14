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
 * Produces and collects values from a [Flow] provided by the [producer] lambda and represents its
 * latest value via retained [State]. Every time there would be a new value posted into the [Flow],
 * the returned [State] will be updated causing recomposition of every [State.value] usage.
 *
 * This function safely produces and collects a Flow by using a single retained state with unified
 * inputs. The Flow is produced within the retained state's producer block, ensuring proper scoping
 * and preventing memory leaks from retaining Flow instances across configuration changes.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [producer] to be rerun
 * @param initial the initial value for the state
 * @param context [CoroutineContext] to use for collecting.
 * @param producer a suspend lambda that provides the [Flow] to collect from
 */
@Composable
public fun <T : R, R> produceAndCollectAsRetainedState(
  vararg inputs: Any?,
  initial: R,
  context: CoroutineContext = EmptyCoroutineContext,
  producer: suspend () -> Flow<T>,
): State<R> =
  produceRetainedState(initial, *inputs) {
    val flow = producer()
    if (context == EmptyCoroutineContext) {
      flow.collect { value = it }
    } else withContext(context) { flow.collect { value = it } }
  }

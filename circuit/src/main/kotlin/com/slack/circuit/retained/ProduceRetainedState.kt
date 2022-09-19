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
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine

private class ProduceRetainedStateScopeImpl<T>(
  state: MutableState<T>,
  override val coroutineContext: CoroutineContext
) : ProduceStateScope<T>, MutableState<T> by state {

  override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
    try {
      suspendCancellableCoroutine<Nothing> {}
    } finally {
      onDispose()
    }
  }
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time without a defined data source.
 *
 * [producer] is launched when [produceRetainedState] enters the composition and is cancelled when
 * [produceRetainedState] leaves the composition. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainedState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceRetainedState(
  initialValue: T,
  @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
  val result = rememberRetained { mutableStateOf(initialValue) }
  LaunchedEffect(Unit) { ProduceRetainedStateScopeImpl(result, coroutineContext).producer() }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1].
 *
 * [producer] is launched when [produceRetainedState] enters the composition and is cancelled when
 * [produceRetainedState] leaves the composition. If [key1] changes, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainedState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceRetainedState(
  initialValue: T,
  key1: Any?,
  @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
  val result = rememberRetained { mutableStateOf(initialValue) }
  LaunchedEffect(key1) { ProduceRetainedStateScopeImpl(result, coroutineContext).producer() }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1] and [key2].
 *
 * [producer] is launched when [produceRetainedState] enters the composition and is cancelled when
 * [produceRetainedState] leaves the composition. If [key1] or [key2] change, a running [producer]
 * will be cancelled and re-launched for the new source. [producer] should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainedState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceRetainedState(
  initialValue: T,
  key1: Any?,
  key2: Any?,
  @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
  val result = rememberRetained { mutableStateOf(initialValue) }
  LaunchedEffect(key1, key2) { ProduceRetainedStateScopeImpl(result, coroutineContext).producer() }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1], [key2] and [key3].
 *
 * [producer] is launched when [produceRetainedState] enters the composition and is cancelled when
 * [produceRetainedState] leaves the composition. If [key1], [key2] or [key3] change, a running
 * [producer] will be cancelled and re-launched for the new source. [producer should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainedState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceRetainedState(
  initialValue: T,
  key1: Any?,
  key2: Any?,
  key3: Any?,
  @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
  val result = rememberRetained { mutableStateOf(initialValue) }
  LaunchedEffect(key1, key2, key3) {
    ProduceRetainedStateScopeImpl(result, coroutineContext).producer()
  }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [keys].
 *
 * [producer] is launched when [produceRetainedState] enters the composition and is cancelled when
 * [produceRetainedState] leaves the composition. If [keys] change, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainedState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceRetainedState(
  initialValue: T,
  vararg keys: Any?,
  @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
  val result = rememberRetained { mutableStateOf(initialValue) }
  @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
  (LaunchedEffect(keys = keys) {
    ProduceRetainedStateScopeImpl(result, coroutineContext).producer()
  })
  return result
}

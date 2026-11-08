// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State

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
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainedState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainedState(
  initialValue: T,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  return me.stagg.retainx.produceRetainState(initialValue = initialValue, producer = producer)
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
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainedState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainedState(
  initialValue: T,
  key1: Any?,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  return me.stagg.retainx.produceRetainState(
    initialValue = initialValue,
    key1 = key1,
    producer = producer,
  )
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
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainedState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainedState(
  initialValue: T,
  key1: Any?,
  key2: Any?,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  return me.stagg.retainx.produceRetainState(
    initialValue = initialValue,
    key1 = key1,
    key2 = key2,
    producer = producer,
  )
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1], [key2] and [key3].
 *
 * [producer] is launched when [produceRetainedState] enters the composition and is cancelled when
 * [produceRetainedState] leaves the composition. If [key1], [key2] or [key3] change, a running
 * [producer] will be cancelled and re-launched for the new source.
 * [producer should use [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainedState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainedState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainedState(
  initialValue: T,
  key1: Any?,
  key2: Any?,
  key3: Any?,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  return me.stagg.retainx.produceRetainState(
    initialValue = initialValue,
    key1 = key1,
    key2 = key2,
    key3 = key3,
    producer = producer,
  )
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
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainedState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainedState(
  initialValue: T,
  vararg keys: Any?,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  return me.stagg.retainx.produceRetainState(
    initialValue = initialValue,
    keys = keys,
    producer = producer,
  )
}

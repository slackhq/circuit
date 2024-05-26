// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("NOTHING_TO_INLINE")

package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter

/**
 * By default [CircuitContent] will wrap presenters so that the last emitted [CircuitUiState] is
 * replayed when the presenter is paused. If this behavior is not wanted, the [Presenter] should
 * implement this interface to disable the behavior.
 */
@Stable public interface NonPausablePresenter<UiState : CircuitUiState> : Presenter<UiState>

/**
 * Presents the UI state when the lifecycle is resumed, otherwise will replay the last emitted UI
 * state.
 *
 * The [CircuitUiState] class returned by the [Presenter] is required to implement [equals] and
 * [hashCode] methods correctly, otherwise this function can create composition loops.
 *
 * @param key A unique key for the pausable state
 * @param isActive Whether the presenter is active or not.
 */
@Composable
public inline fun <UiState : CircuitUiState> Presenter<UiState>.presentWithLifecycle(
  key: String? = null,
  isActive: Boolean = LocalRecordLifecycle.current.isActive,
): UiState = pausableState(key, isActive) { present() }

/**
 * Wraps a composable state producer, which will replay the last emitted state instance when
 * [isActive] is `false`.
 *
 * The class [T] returned from [produceState] is required to implement [equals] and [hashCode]
 * methods correctly, otherwise this function can create composition loops.
 *
 * @param key A unique key for the pausable state.
 * @param isActive Whether the state producer should be active or not.
 * @param produceState A composable lambda function which produces the state
 */
@Composable
public fun <T> pausableState(
  key: String? = null,
  isActive: Boolean = LocalRecordLifecycle.current.isActive,
  produceState: @Composable () -> T,
): T {
  var state: T? by remember(key) { mutableStateOf(null) }

  val saveableStateHolder = rememberSaveableStateHolder()

  if (isActive || state == null) {
    val retainedStateRegistry = rememberRetained(key = key) { RetainedStateRegistry() }
    CompositionLocalProvider(LocalRetainedStateRegistry provides retainedStateRegistry) {
      saveableStateHolder.SaveableStateProvider(key = key ?: "pausable_state") {
        state = produceState()
      }
    }
  }

  return state!!
}

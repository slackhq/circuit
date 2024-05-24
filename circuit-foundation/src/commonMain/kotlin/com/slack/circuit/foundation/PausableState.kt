// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
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
 * replayed when the presenter is paused. If this behavior is not wanted, [Presenter] should
 * implement this interface to disable the behavior.
 */
@Stable public interface NonPausablePresenter<UiState : CircuitUiState> : Presenter<UiState>

@Composable
public fun <UiState : CircuitUiState> Presenter<UiState>.presentWithLifecycle(
  isResumed: Boolean = LocalLifecycle.current.isResumed
): UiState = pausableState(isResumed) { present() }

@Composable
public fun <UiState : CircuitUiState> pausableState(
  isResumed: Boolean = LocalLifecycle.current.isResumed,
  produceState: @Composable () -> UiState,
): UiState {
  var uiState: UiState? by remember { mutableStateOf(null) }

  val saveableStateHolder = rememberSaveableStateHolder()

  if (isResumed || uiState == null) {
    val retainedStateRegistry = rememberRetained { RetainedStateRegistry() }
    CompositionLocalProvider(LocalRetainedStateRegistry provides retainedStateRegistry) {
      saveableStateHolder.SaveableStateProvider("pausable_state") { uiState = produceState() }
    }
  }

  return uiState!!
}

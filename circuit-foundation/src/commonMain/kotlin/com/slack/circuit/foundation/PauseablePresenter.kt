// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter

public abstract class PauseablePresenter<UiState : CircuitUiState>() : Presenter<UiState> {

  private var uiState by mutableStateOf<UiState?>(null)

  @Composable
  override fun present(): UiState {
    val lifecycle = LocalLifecycle.current
    val saveableStateHolder = rememberSaveableStateHolder()

    if (lifecycle.isResumed || uiState == null) {
      val retainedStateRegistry = rememberRetained { RetainedStateRegistry() }
      CompositionLocalProvider(LocalRetainedStateRegistry provides retainedStateRegistry) {
        saveableStateHolder.SaveableStateProvider("pauseable_presenter") { uiState = _present() }
      }
    }

    return uiState!!
  }

  @Composable protected abstract fun _present(): UiState
}

public fun <UiState : CircuitUiState> Presenter<UiState>.toPauseablePresenter():
  PauseablePresenter<UiState> {
  if (this is PauseablePresenter<UiState>) return this
  // Else we wrap the presenter
  return object : PauseablePresenter<UiState>() {
    @Composable override fun _present(): UiState = this@toPauseablePresenter.present()
  }
}

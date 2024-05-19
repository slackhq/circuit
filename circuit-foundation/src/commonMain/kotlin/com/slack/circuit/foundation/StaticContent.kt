// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import kotlin.LazyThreadSafetyMode.NONE

/** TODO doc */
@Suppress("UNCHECKED_CAST")
public fun <UiState : CircuitUiState> statelessPresenter(): Presenter<UiState> =
  STATELESS as Presenter<UiState>

/**
 * Simple presenter that always returns the same [computedState]. This is useful for UIs that are
 * static and only ever use their initial state.
 *
 * For UIs that are completely stateless, use [Circuit.Builder.addStaticUi] or [statelessPresenter].
 */
internal class StaticPresenter<UiState : CircuitUiState>(computeState: () -> UiState) :
  Presenter<CircuitUiState> {

  private val computedState: UiState by lazy(NONE, computeState)

  @ReadOnlyComposable @Composable override fun present(): UiState = computedState
}

/**
 * Singleton instance of a stateless presenter, which is a [StaticPresenter] that produces no usable
 * state. This is never actually used at runtime, just exists for internal wiring convenience.
 */
private val STATELESS = StaticPresenter<CircuitUiState> { StatelessUiState }

@Immutable internal object StatelessUiState : CircuitUiState

/**
 * Simple marker class so we can optimize in [CircuitContent] when the UI is static. This allows us
 * to check for it and avoid ever calling the presenter.
 */
internal class StaticUi(private val delegate: @Composable (Modifier) -> Unit) :
  Ui<StatelessUiState> {

  @Composable fun Content(modifier: Modifier) = Content(StatelessUiState, modifier)

  @Composable
  override fun Content(state: StatelessUiState, modifier: Modifier) {
    delegate(modifier)
  }
}

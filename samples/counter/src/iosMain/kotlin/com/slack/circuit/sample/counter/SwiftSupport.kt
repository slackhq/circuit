// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import kotlinx.coroutines.flow.Flow

fun newCounterPresenter() = presenterOf { CounterPresenter(Navigator.NoOp) }

fun <UiState : CircuitUiState> Presenter<UiState>.asSwiftPresenter(): SwiftPresenter<UiState> {
  return SwiftPresenter(this)
}

// Adapted from the KotlinConf app
// https://github.com/JetBrains/kotlinconf-app/blob/642404f3454d384be966c34d6b254b195e8d2892/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/utils/Coroutines.kt#L6
// No interface because interfaces don't support generics in Kotlin/Native.
// TODO let's try to generify this pattern somehow.
class SwiftPresenter<UiState : CircuitUiState>
internal constructor(
  private val delegate: Presenter<UiState>,
) {
  // TODO StateFlow would be nice here
  val state: Flow<UiState> = moleculeFlow(RecompositionMode.Immediate) { delegate.present() }
}

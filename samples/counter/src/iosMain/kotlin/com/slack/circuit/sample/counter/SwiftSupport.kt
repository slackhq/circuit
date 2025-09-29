// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow

fun newCounterPresenter() = presenterOf { CounterPresenter(Navigator.NoOp) }

// Adapted from the KotlinConf app
// https://github.com/JetBrains/kotlinconf-app/blob/642404f3454d384be966c34d6b254b195e8d2892/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/utils/Coroutines.kt#L6
// No interface because interfaces don't support generics in Kotlin/Native.
// TODO let's try to generify this pattern somehow.
class SupportSwiftPresenter<UiState : CircuitUiState>(
  private val delegate: Presenter<UiState>,
  // TODO what's the right thing here? Can we get a scope from the UI? Should it be exposed via
  //  Circuit?
  scope: CoroutineScope,
) {
  constructor(delegate: Presenter<UiState>) : this(delegate, MainScope())

  @NativeCoroutinesState
  val state: StateFlow<UiState> =
    scope.launchMolecule(RecompositionMode.Immediate) { delegate.present() }
}

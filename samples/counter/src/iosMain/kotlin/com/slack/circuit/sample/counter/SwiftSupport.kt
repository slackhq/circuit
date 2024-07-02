// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.ObjCObjectBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.objc.objc_lookUpClass
import platform.objc.object_getClass

fun newCounterPresenter() = presenterOf { CounterPresenter(Navigator.NoOp) }

// Adapted from the KotlinConf app
// https://github.com/JetBrains/kotlinconf-app/blob/642404f3454d384be966c34d6b254b195e8d2892/shared/src/commonMain/kotlin/org/jetbrains/kotlinconf/utils/Coroutines.kt#L6
// No interface because interfaces don't support generics in Kotlin/Native.
// TODO let's try to generify this pattern somehow.
class FlowPresenter<UiState : CircuitUiState>(
  private val delegate: Presenter<UiState>,
  // TODO what's the right thing here? Can we get a scope from the UI? Should it be exposed via
  //  Circuit?
  scope: CoroutineScope,
) {
  constructor(delegate: Presenter<UiState>) : this(delegate, MainScope())

  val state: StateFlow<UiState> =
    scope.launchMolecule(RecompositionMode.Immediate) { delegate.present() }
}

/**
 * Delegate factory is a factory for a swift function that takes a support swift presenter as a
 * param and returns a UIViewController.
 */
@OptIn(ExperimentalForeignApi::class)
class SwiftUi<UiState : CircuitUiState>(
  // TODO gets created with what? A FlowPresenter<UiState>? Regular circuit presenter? Should this
  //  handle creating the UIViewController?
  private val delegateFactory: (FlowPresenter<UiState>) -> UIViewController
) : Ui<UiState> {
  @Composable
  override fun Content(state: UiState, modifier: Modifier) {
    val updatedState by rememberUpdatedState(state)
    val delegatePresenter = remember { FlowPresenter(presenterOf { updatedState }) }
    UIKitViewController(
      modifier = modifier,
      factory = { delegateFactory(delegatePresenter) },
    )
  }
}

fun <UiState : CircuitUiState> uiViewControllerOf(viewFactory: () -> UIView): UIViewController {
  //  - Need
  TODO()
}

class SwiftUiScreen : Screen

// TODO work this API a little more
val swiftUiFactory = Ui.Factory { s, _ ->
  when (s) {
    is SwiftUiScreen -> {
      SwiftUi<CounterScreen.State> { presenter ->
        // TODO need to expose SwiftPresenterFactory from Swift
        val swiftPresenter = SwiftPresenterFactory().create(presenter)
        uiViewControllerOf {
          // Instantiate your View with the presenter
          // TODO
          //  - Need a way to instantiate the view. Possibly from a Swift function?
          CounterView(swiftPresenter)
        }
      }
    }
    else -> null
  }
}

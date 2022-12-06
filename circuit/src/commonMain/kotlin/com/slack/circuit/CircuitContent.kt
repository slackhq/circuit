// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

@Composable
public fun CircuitContent(
  screen: Screen,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuitConfig.onUnavailableContent,
) {
  CircuitContent(screen, Navigator.NoOp, circuitConfig, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  onNavEvent: (event: NavEvent) -> Unit,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuitConfig.onUnavailableContent,
) {
  val navigator =
    remember(onNavEvent) {
      object : Navigator {
        override fun goTo(screen: Screen) {
          onNavEvent(GoToNavEvent(screen))
        }

        override fun pop(): Screen? {
          onNavEvent(PopNavEvent)
          return null
        }
      }
    }
  CircuitContent(screen, navigator, circuitConfig, unavailableContent)
}

@Composable
internal fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  circuitConfig: CircuitConfig,
  unavailableContent: (@Composable (screen: Screen) -> Unit)?,
) {
  val context = remember(screen, navigator, circuitConfig) { CircuitContext(circuitConfig) }
  val eventListener =
    remember(screen, context) {
      circuitConfig.eventListenerFactory?.create(screen, context) ?: EventListener.NONE
    }

  eventListener.onBeforeCreatePresenter(screen, navigator, context)
  @Suppress("UNCHECKED_CAST")
  val presenter = circuitConfig.presenter(screen, navigator) as Presenter<CircuitUiState>?
  eventListener.onAfterCreatePresenter(screen, navigator, presenter, context)

  eventListener.onBeforeCreateUi(screen, context)
  val screenUi = circuitConfig.ui(screen)
  eventListener.onAfterCreateUi(screen, screenUi, context)

  if (screenUi != null && presenter != null) {
    @Suppress("UNCHECKED_CAST")
    CircuitContent(screen, eventListener, presenter, screenUi.ui as Ui<CircuitUiState>)
  } else if (unavailableContent != null) {
    unavailableContent(screen)
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState : CircuitUiState> CircuitContent(
  screen: Screen,
  eventListener: EventListener,
  presenter: Presenter<UiState>,
  ui: Ui<UiState>,
) {
  DisposableEffect(screen) {
    eventListener.onStartPresent()

    onDispose { eventListener.onDisposePresent() }
  }
  val state = presenter.present()
  // TODO not sure why stateFlow + LaunchedEffect + distinctUntilChanged doesn't work here
  SideEffect { eventListener.onState(state) }
  DisposableEffect(screen) {
    eventListener.onStartContent()

    onDispose { eventListener.onDisposeContent() }
  }
  ui.Content(state)
}

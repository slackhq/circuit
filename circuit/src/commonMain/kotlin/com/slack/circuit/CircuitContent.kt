// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
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
  val screenUi = circuitConfig.ui(screen)

  @Suppress("UNCHECKED_CAST")
  val presenter = circuitConfig.presenter(screen, navigator) as Presenter<CircuitUiState>?

  if (screenUi != null && presenter != null) {
    val eventListener = rememberEventListener(screen) {
      circuitConfig.eventListenerFactory?.create(screen) ?: EventListener.NONE
    }
    @Suppress("UNCHECKED_CAST")
    CircuitContent(eventListener, presenter, screenUi.ui as Ui<CircuitUiState>)
  } else if (unavailableContent != null) {
    unavailableContent(screen)
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState : CircuitUiState> CircuitContent(
  eventListener: EventListener,
  presenter: Presenter<UiState>,
  ui: Ui<UiState>,
) {
  val state = presenter.present()
  // TODO not sure why stateFlow + LaunchedEffect + distinctUntilChanged doesn't work here
  SideEffect { eventListener.onState(state) }
  ui.Content(state)
}

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
  return CircuitContent(ScreenRequest.Builder(screen).build(), circuitConfig, unavailableContent)
}

@Composable
public fun CircuitContent(
  request: ScreenRequest,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuitConfig.onUnavailableContent,
) {
  CircuitContent(request, Navigator.NoOp, circuitConfig, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  onNavEvent: (event: NavEvent) -> Unit,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuitConfig.onUnavailableContent,
) {
  CircuitContent(
    ScreenRequest.Builder(screen).build(),
    onNavEvent,
    circuitConfig,
    unavailableContent
  )
}

@Composable
public fun CircuitContent(
  request: ScreenRequest,
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
  CircuitContent(request, navigator, circuitConfig, unavailableContent)
}

@Composable
internal fun CircuitContent(
  request: ScreenRequest,
  navigator: Navigator,
  circuitConfig: CircuitConfig,
  unavailableContent: (@Composable (screen: Screen) -> Unit)?,
) {
  val screenUi = circuitConfig.ui(request)

  @Suppress("UNCHECKED_CAST")
  val presenter = circuitConfig.presenter(request, navigator) as Presenter<CircuitUiState>?

  if (screenUi != null && presenter != null) {
    val eventListener = circuitConfig.eventListenerFactory?.create(request) ?: EventListener.NONE
    @Suppress("UNCHECKED_CAST")
    CircuitContent(eventListener, presenter, screenUi.ui as Ui<CircuitUiState>)
  } else if (unavailableContent != null) {
    unavailableContent(request.screen)
  } else {
    error("Could not render screen $request")
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

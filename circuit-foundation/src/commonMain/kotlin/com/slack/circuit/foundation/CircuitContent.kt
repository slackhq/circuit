// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui

@Composable
public fun CircuitContent(
  screen: Screen,
  modifier: Modifier = Modifier,
  circuitConfig: CircuitConfig = requireNotNull(LocalCircuitConfig.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuitConfig.onUnavailableContent,
) {
  CircuitContent(screen, Navigator.NoOp, modifier, circuitConfig, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  modifier: Modifier = Modifier,
  onNavEvent: (event: NavEvent) -> Unit,
  circuitConfig: CircuitConfig = requireNotNull(LocalCircuitConfig.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuitConfig.onUnavailableContent,
) {
  val navigator =
    remember(onNavEvent) {
      object : Navigator {
        override fun goTo(screen: Screen) {
          onNavEvent(NavEvent.GoTo(screen))
        }

        override fun resetRoot(newRoot: Screen): List<Screen> {
          onNavEvent(NavEvent.ResetRoot(newRoot))
          return emptyList()
        }

        override fun pop(): Screen? {
          onNavEvent(NavEvent.Pop)
          return null
        }
      }
    }
  CircuitContent(screen, navigator, modifier, circuitConfig, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  modifier: Modifier = Modifier,
  circuitConfig: CircuitConfig = requireNotNull(LocalCircuitConfig.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuitConfig.onUnavailableContent,
) {
  val parent = LocalCircuitContext.current
  @OptIn(InternalCircuitApi::class)
  val context =
    remember(screen, navigator, circuitConfig, parent) {
      CircuitContext(parent).also { it.config = circuitConfig }
    }
  CompositionLocalProvider(LocalCircuitContext provides context) {
    CircuitContent(screen, modifier, navigator, circuitConfig, unavailableContent, context)
  }
}

@Composable
internal fun CircuitContent(
  screen: Screen,
  modifier: Modifier,
  navigator: Navigator,
  circuitConfig: CircuitConfig,
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit),
  context: CircuitContext,
) {
  val eventListener =
    remember(screen, context) {
      val eventListener = circuitConfig.eventListenerFactory?.create(screen, context) ?: EventListener.NONE
      EventListenerRememberObserver(eventListener)
    }

  val presenter =
    remember(eventListener, screen, navigator, context) {
      eventListener.onBeforeCreatePresenter(screen, navigator, context)
      @Suppress("UNCHECKED_CAST")
      (circuitConfig.presenter(screen, navigator, context) as Presenter<CircuitUiState>?).also {
        eventListener.onAfterCreatePresenter(screen, navigator, it, context)
      }
    }

  val ui =
    remember(eventListener, screen, context) {
      eventListener.onBeforeCreateUi(screen, context)
      circuitConfig.ui(screen, context).also { ui ->
        eventListener.onAfterCreateUi(screen, ui, context)
      }
    }

  if (ui != null && presenter != null) {
    @Suppress("UNCHECKED_CAST")
    (CircuitContent(screen, modifier, eventListener, presenter, ui as Ui<CircuitUiState>))
  } else {
    eventListener.onUnavailableContent(screen, presenter, ui, context)
    unavailableContent(screen, modifier)
  }
}

@Composable
private fun <UiState : CircuitUiState> CircuitContent(
  screen: Screen,
  modifier: Modifier,
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
  ui.Content(state, modifier)
}

private class EventListenerRememberObserver(private val eventListener: EventListener) : RememberObserver, EventListener by eventListener {
  override fun onAbandoned() = Unit // Do nothing. Never started

  override fun onForgotten() = eventListener.dispose()

  override fun onRemembered() = eventListener.start()
}

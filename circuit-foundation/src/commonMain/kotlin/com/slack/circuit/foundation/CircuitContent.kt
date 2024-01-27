// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.DelicateCircuitApi
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui

@Composable
public fun CircuitContent(
  screen: Screen,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  CircuitContent(screen, Navigator.NoOp, modifier, circuit, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  modifier: Modifier = Modifier,
  onNavEvent: (event: NavEvent) -> Unit,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  val localNavigator = LocalNavigator.current
  val navigator =
    remember(onNavEvent, localNavigator) {
      object : Navigator {
        @DelicateCircuitApi
        override val backStack: BackStack<out BackStack.Record>? = localNavigator?.backStack

        override fun goTo(screen: Screen) {
          onNavEvent(NavEvent.GoTo(screen))
        }

        override fun resetRoot(newRoot: Screen): List<Screen> {
          onNavEvent(NavEvent.ResetRoot(newRoot))
          return emptyList()
        }

        override fun pop(result: PopResult?): Screen? {
          onNavEvent(NavEvent.Pop(result))
          return null
        }
      }
    }
  CircuitContent(screen, navigator, modifier, circuit, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  val parent = LocalCircuitContext.current
  @OptIn(InternalCircuitApi::class)
  val context =
    remember(screen, navigator, circuit, parent) {
      CircuitContext(parent).also { it.circuit = circuit }
    }
  CompositionLocalProvider(LocalCircuitContext provides context) {
    CircuitContent(screen, modifier, navigator, circuit, unavailableContent, context)
  }
}

@Composable
internal fun CircuitContent(
  screen: Screen,
  modifier: Modifier,
  navigator: Navigator,
  circuit: Circuit,
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit),
  context: CircuitContext,
) {
  val eventListener = rememberEventListener(screen, context, factory = circuit.eventListenerFactory)
  DisposableEffect(eventListener, screen, context) { onDispose { eventListener.dispose() } }

  val presenter = rememberPresenter(screen, navigator, context, eventListener, circuit::presenter)

  val ui = rememberUi(screen, context, eventListener, circuit::ui)

  if (ui != null && presenter != null) {
    (CircuitContent(screen, modifier, presenter, ui, eventListener))
  } else {
    eventListener.onUnavailableContent(screen, presenter, ui, context)
    unavailableContent(screen, modifier)
  }
}

@Composable
public fun <UiState : CircuitUiState> CircuitContent(
  screen: Screen,
  modifier: Modifier,
  presenter: Presenter<UiState>,
  ui: Ui<UiState>,
  eventListener: EventListener = EventListener.NONE,
) {
  DisposableEffect(screen) {
    eventListener.onStartPresent()

    onDispose { eventListener.onDisposePresent() }
  }

  // While the presenter is different, in the eyes of compose its position is _the same_, meaning
  // we need to wrap the presenter in a key() to force recomposition if it changes. A good example
  // case of this is when you have code that calls CircuitContent with a common screen with
  // different inputs (but thus same presenter instance type) and you need this to recompose with a
  // different presenter.
  val state = key(screen) { presenter.present() }

  // TODO not sure why stateFlow + LaunchedEffect + distinctUntilChanged doesn't work here
  SideEffect { eventListener.onState(state) }
  DisposableEffect(screen) {
    eventListener.onStartContent()

    onDispose { eventListener.onDisposeContent() }
  }
  ui.Content(state, modifier)
}

/**
 * Remembers a new [EventListener] instance for the given [screen] and [context].
 *
 * @param startOnInit indicates whether to call [EventListener.start] automatically after
 *   instantiation. True by default.
 * @param factory a factory to create the [EventListener].
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
public inline fun rememberEventListener(
  screen: Screen,
  context: CircuitContext = CircuitContext.EMPTY,
  startOnInit: Boolean = true,
  factory: EventListener.Factory? = null,
): EventListener {
  return remember(screen, context) {
    (factory?.create(screen, context) ?: EventListener.NONE).also {
      if (startOnInit) {
        it.start()
      }
    }
  }
}

/**
 * Remembers a new [Presenter] instance for the given [screen], [navigator], [context], and
 * [eventListener].
 *
 * @param factory a factory to create the [Presenter].
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
public inline fun rememberPresenter(
  screen: Screen,
  navigator: Navigator = Navigator.NoOp,
  context: CircuitContext = CircuitContext.EMPTY,
  eventListener: EventListener = EventListener.NONE,
  factory: Presenter.Factory,
): Presenter<CircuitUiState>? =
  remember(eventListener, screen, navigator, context) {
    eventListener.onBeforeCreatePresenter(screen, navigator, context)
    @Suppress("UNCHECKED_CAST")
    (factory.create(screen, navigator, context) as Presenter<CircuitUiState>?).also {
      eventListener.onAfterCreatePresenter(screen, navigator, it, context)
    }
  }

/**
 * Remembers a new [Ui] instance for the given [screen], [context], and [eventListener].
 *
 * @param factory a factory to create the [Ui].
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
public inline fun rememberUi(
  screen: Screen,
  context: CircuitContext = CircuitContext.EMPTY,
  eventListener: EventListener = EventListener.NONE,
  factory: Ui.Factory,
): Ui<CircuitUiState>? =
  remember(eventListener, screen, context) {
    eventListener.onBeforeCreateUi(screen, context)
    @Suppress("UNCHECKED_CAST")
    (factory.create(screen, context) as Ui<CircuitUiState>?).also { ui ->
      eventListener.onAfterCreateUi(screen, ui, context)
    }
  }

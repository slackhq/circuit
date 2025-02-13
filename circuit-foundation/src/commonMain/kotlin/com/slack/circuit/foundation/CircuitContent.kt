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
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavigationContext
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun CircuitContent(
  screen: Screen,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
  key: Any? = screen,
) {
  CircuitContent(screen, Navigator.NoOp, modifier, circuit, unavailableContent, key)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  modifier: Modifier = Modifier,
  onNavEvent: (event: NavEvent) -> Unit,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
  key: Any? = screen,
) {
  val navigator =
    remember(onNavEvent) {
      object : Navigator {
        override fun goTo(screen: Screen, context: NavigationContext): Boolean {
          onNavEvent(NavEvent.GoTo(screen, context = context))
          return true
        }

        override fun resetRoot(
          newRoot: Screen,
          saveState: Boolean,
          restoreState: Boolean,
          context: NavigationContext,
        ): ImmutableList<Screen> {
          onNavEvent(NavEvent.ResetRoot(newRoot, saveState, restoreState, context = context))
          return persistentListOf()
        }

        override fun pop(result: PopResult?, context: NavigationContext): Screen? {
          onNavEvent(NavEvent.Pop(result, context = context))
          return null
        }

        override fun peek(): Screen = screen

        override fun peekBackStack(): ImmutableList<Screen> = persistentListOf(screen)
      }
    }
  CircuitContent(screen, navigator, modifier, circuit, unavailableContent, key)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  modifier: Modifier = Modifier,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  unavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
  key: Any? = screen,
) {
  val parent = LocalCircuitContext.current
  @OptIn(InternalCircuitApi::class)
  val context =
    remember(screen, navigator, circuit, parent) {
      CircuitContext(parent).also { it.circuit = circuit }
    }
  CompositionLocalProvider(LocalCircuitContext provides context) {
    CircuitContent(screen, modifier, navigator, circuit, unavailableContent, context, key)
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
  key: Any? = screen,
) {
  val eventListener = rememberEventListener(screen, context, factory = circuit.eventListenerFactory)
  DisposableEffect(eventListener, screen, context) { onDispose { eventListener.dispose() } }

  val presenter = rememberPresenter(screen, navigator, context, eventListener, circuit::presenter)

  val ui = rememberUi(screen, context, eventListener, circuit::ui)

  if (ui != null && presenter != null) {
    (CircuitContent(screen, presenter, ui, modifier, eventListener, key))
  } else {
    eventListener.onUnavailableContent(screen, presenter, ui, context)
    unavailableContent(screen, modifier)
  }
}

@Composable
public fun <UiState : CircuitUiState> CircuitContent(
  screen: Screen,
  presenter: Presenter<UiState>,
  ui: Ui<UiState>,
  modifier: Modifier = Modifier,
  eventListener: EventListener = EventListener.NONE,
  key: Any? = screen,
  presentWithLifecycle: Boolean = LocalCircuit.current?.presentWithLifecycle == true,
): Unit =
  // While the screen is different, in the eyes of compose its position is _the same_, meaning
  // we need to wrap the ui and presenter in a key() to force recomposition if it changes. A good
  // example case of this is when you have code that calls CircuitContent with a common screen with
  // different inputs (but thus same presenter instance type) and you need this to recompose with
  // a different presenter.
  key(key) {
    // Static UI can be rendered directly, no presenter ever needs to be connected
    if (ui is StaticUi) {
      // TODO anything we want to send to the event listener here for state?
      DisposableEffect(screen) {
        eventListener.onStartContent()
        onDispose(eventListener::onDisposeContent)
      }
      ui.Content(modifier)
    } else {
      DisposableEffect(screen) {
        eventListener.onStartPresent()
        onDispose(eventListener::onDisposePresent)
      }

      val state =
        when {
          presentWithLifecycle && presenter !is NonPausablePresenter<UiState> ->
            presenter.presentWithLifecycle()
          else -> presenter.present()
        }

      // TODO not sure why stateFlow + LaunchedEffect + distinctUntilChanged doesn't work here
      SideEffect { eventListener.onState(state) }
      DisposableEffect(screen) {
        eventListener.onStartContent()
        onDispose(eventListener::onDisposeContent)
      }
      ui.Content(state, modifier)
    }
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

package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.CircuitInterceptingNavigator.FailureNotifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * An implementation of [NavigableCircuitContent] that allows for a [CircuitNavigationInterceptor]
 * to intercept navigation.
 *
 * @param onRootPop Handle the root pop. On Android this is handled by the system back handler if
 *   left as null.
 */
@Composable
public fun CircuitInterceptingNavigableContent(
  screens: ImmutableList<Screen>,
  interceptors: ImmutableList<CircuitNavigationInterceptor>,
  modifier: Modifier = Modifier,
  eventListeners: ImmutableList<CircuitNavigationEventListener> = persistentListOf(),
  notifier: FailureNotifier? = null,
  onRootPop: ((result: PopResult?) -> Unit)? = null,
  circuit: Circuit = requireNotNull(LocalCircuit.current),
  decoration: NavDecoration = circuit.defaultNavDecoration,
  unavailableRoute: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    circuit.onUnavailableContent,
) {
  check(screens.isNotEmpty()) { "No screens were provided." }
  val backStack = rememberSaveableBackStack(screens)
  BackStackChangedEffect(eventListeners, backStack)
  // Build the delegate Navigator.
  val backstackNavigator = rememberCircuitInterceptingBackStackNavigator(backStack, onRootPop)
  // Handle our NavigationInterceptors.
  val interceptingNavigator =
    rememberCircuitInterceptingNavigator(
      navigator = backstackNavigator,
      interceptors = interceptors,
      eventListeners = eventListeners,
      notifier = notifier,
    )
  NavigableCircuitContent(
    navigator = interceptingNavigator,
    backStack = backStack,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

/**
 * Provides a [Navigator] that is delegated to by the [CircuitInterceptingNavigator] if navigation
 * was not intercepted by a [CircuitNavigationInterceptor].
 */
@Composable
public expect fun rememberCircuitInterceptingBackStackNavigator(
  backStack: SaveableBackStack,
  onRootPop: ((result: PopResult?) -> Unit)?,
): Navigator

/** A SideEffect that notifies the [CircuitNavigationEventListener] when the backstack changes. */
@Composable
public fun BackStackChangedEffect(
  eventListeners: ImmutableList<CircuitNavigationEventListener>,
  backStack: SaveableBackStack,
) {
  // Key using the screen as it'll be the same through rotation, as the record key will change.
  val screens = backStack.map { it.screen }.toImmutableList()
  rememberRetained(screens) {
    val backStackScreens = backStack.map { it.screen }.toImmutableList()
    eventListeners.forEach { it.onBackStackChanged(backStackScreens) }
  }
}

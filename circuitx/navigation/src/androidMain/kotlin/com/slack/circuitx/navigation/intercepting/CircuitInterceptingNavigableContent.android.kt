package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Navigator
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult

/**
 * Provides a [Navigator] that is delegated to by the [CircuitInterceptingNavigator] if navigation
 * was not intercepted by a [CircuitNavigationInterceptor].
 *
 *
 * @param onRootPop A handler for the root pop. Null indicates that the root pop should be called by
 *   the system back handler.
 */
@Composable
public actual fun rememberCircuitInterceptingBackStackNavigator(
  backStack: SaveableBackStack,
  onRootPop: ((result: PopResult?) -> Unit)?,
): Navigator {
  return if (onRootPop != null) {
    rememberCircuitNavigator(backStack = backStack, onRootPop = onRootPop)
  } else {
    rememberCircuitNavigator(backStack = backStack, enableBackHandler = true)
  }
}

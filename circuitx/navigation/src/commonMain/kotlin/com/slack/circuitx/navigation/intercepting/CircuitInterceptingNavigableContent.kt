// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.CircuitInterceptingNavigator.FailureNotifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
  // Build the delegate Navigator.
  val interceptingNavigator =
    rememberCircuitInterceptingNavigator(
      backStack = backStack,
      interceptors = interceptors,
      eventListeners = eventListeners,
      notifier = notifier,
      onRootPop = onRootPop,
    )
  NavigableCircuitContent(
    navigator = interceptingNavigator,
    backStack = backStack,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

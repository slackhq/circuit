// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult

/**
 * Provides a [Navigator] that is delegated to by the [CircuitInterceptingNavigator] if navigation
 * was not intercepted by a [CircuitNavigationInterceptor].
 */
@Composable
public actual fun rememberCircuitInterceptingBackStackNavigator(
  backStack: SaveableBackStack,
  onRootPop: ((result: PopResult?) -> Unit)?,
): Navigator {
  return rememberCircuitNavigator(backStack, onRootPop = onRootPop ?: {})
}

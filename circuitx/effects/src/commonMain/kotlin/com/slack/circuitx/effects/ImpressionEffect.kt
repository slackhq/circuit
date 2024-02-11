// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/**
 * A side effect that will run an [impression]. This [impression] will run only once until it is
 * forgotten based on the current [RetainedStateRegistry] or until the [inputs] change. This is
 * useful for single fire side effects like logging or analytics.
 *
 * @param inputs A set of inputs that when changed will cause the [impression] to be re-run.
 * @param impression The side effect to run.
 */
@Composable
public fun ImpressionEffect(vararg inputs: Any?, impression: () -> Unit) {
  rememberRetained(*inputs, init = impression)
}

/**
 * A [LaunchedEffect] that will run a suspendable [impression]. The [impression] will run once until
 * it is forgotten based on the [RetainedStateRegistry], and/or until the [inputs] change. This is
 * useful for async single fire side effects like logging or analytics.
 *
 * @param inputs A set of inputs that when changed will cause the [impression] to be re-run.
 * @param impression The impression side effect to run.
 */
@Composable
public fun LaunchedImpressionEffect(vararg inputs: Any?, impression: suspend () -> Unit) {
  var impressed by rememberRetained(*inputs) { mutableStateOf(false) }
  LaunchedEffect(*inputs) {
    val wasImpressed = impressed
    impressed = true
    if (!wasImpressed) {
      impression()
    }
  }
}

/**
 * A [LaunchedImpressionEffect] that will also re-run the [impression] if it re-enters the
 * composition after a navigation event. Navigation should be performed on the returned [Navigator]
 * and not the provided [navigator]. This is useful for async single fire side effects like logging
 * or analytics that need to be navigation aware.
 *
 * @param inputs A set of inputs that when changed will cause the [impression] to be re-run.
 * @param navigator The original [Navigator] to delegate navigation events to.
 * @param impression The impression side effect to run.
 */
@Composable
public fun rememberImpressionNavigator(
  vararg inputs: Any?,
  navigator: Navigator,
  impression: suspend () -> Unit,
): Navigator {
  var navigated by rememberRetained { mutableIntStateOf(0) }
  val navigatedInput = remember { navigated }
  LaunchedImpressionEffect(navigatedInput, *inputs, impression = impression)
  return remember(navigator) { OnNavEventNavigator(navigator) { navigated++ } }
}

private class OnNavEventNavigator(val delegate: Navigator, val onNavEvent: () -> Unit) : Navigator {
  override fun goTo(screen: Screen) {
    onNavEvent()
    delegate.goTo(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    onNavEvent()
    return delegate.pop()
  }

  override fun peek(): Screen? = delegate.peek()

  override fun peekBackStack(): ImmutableList<Screen> = delegate.peekBackStack()

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): ImmutableList<Screen> {
    onNavEvent()
    return delegate.resetRoot(newRoot, saveState, restoreState)
  }
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/** Listener that will be notified of Circuit navigation changes. */
public interface CircuitNavigationEventListener {

  /**
   * Called when a back stack has changed. Will be called with the initial state and any other
   * following operation that mutates the back stack.
   */
  public fun onBackStackChanged(backStack: ImmutableList<Screen>) {}

  /**
   * Called when the [CircuitInterceptingNavigator] goes to the [Screen].
   *
   * This is not called if navigation was intercepted.
   *
   * @see CircuitInterceptingNavigator.goTo
   */
  public fun goTo(screen: Screen) {}

  /**
   * Called when the [CircuitInterceptingNavigator] pops the [backStack].
   *
   * This is not called if navigation was intercepted.
   *
   * @see CircuitInterceptingNavigator.pop
   */
  public fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {}

  /**
   * Called when the [CircuitInterceptingNavigator] resets the back stack to [newRoot].
   *
   * This is not called if navigation was intercepted.
   *
   * @see CircuitInterceptingNavigator.resetRoot
   */
  public fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean) {}
}

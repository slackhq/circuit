// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/** Listener that will be notified of Circuit navigation changes. */
public interface NavigationEventListener {

  /**
   * Called when a back stack has changed. Will be called with the initial state and any other
   * following operation that mutates the back stack.
   */
  public fun onBackStackChanged(backStack: List<Screen>) {}

  /**
   * Called when the [InterceptingNavigator] goes to the [Screen].
   *
   * This is not called if navigation was intercepted.
   *
   * @see InterceptingNavigator.goTo
   */
  public fun goTo(screen: Screen) {}

  /**
   * Called when the [InterceptingNavigator] pops the [backStack].
   *
   * This is not called if navigation was intercepted.
   *
   * @see InterceptingNavigator.pop
   */
  public fun pop(backStack: List<Screen>, result: PopResult?) {}

  /**
   * Called when the [InterceptingNavigator] resets the back stack to [newRoot].
   *
   * This is not called if navigation was intercepted.
   *
   * @see InterceptingNavigator.resetRoot
   */
  public fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean) {}
}

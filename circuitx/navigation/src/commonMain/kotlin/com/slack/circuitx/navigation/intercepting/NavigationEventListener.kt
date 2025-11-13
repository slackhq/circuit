// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/** Listener that will be notified of Circuit navigation changes. */
public interface NavigationEventListener {

  /**
   * Called when a back stack has changed. Will be called with the initial state and any other
   * following operation that mutates the back stack.
   *
   * @param backStack The state of the back stack after the change.
   */
  public fun onBackStackChanged(
    backStack: List<Screen>,
    navigationContext: NavigationContext = NoOpNavigationContext,
  ) {}

  /**
   * Called when the [InterceptingNavigator] goes to the [screen].
   *
   * This is not called if navigation was intercepted.
   *
   * @param screen The screen that was navigated to.
   * @see InterceptingNavigator.goTo
   */
  public fun goTo(screen: Screen, navigationContext: NavigationContext = NoOpNavigationContext) {}

  /**
   * Called when the [InterceptingNavigator] pops the back stack.
   *
   * This is not called if navigation was intercepted.
   *
   * @param peekBackStack The state of the back stack before the pop operation.
   * @param result The optional pop result passed to [Navigator.pop].
   * @see InterceptingNavigator.pop
   */
  public fun pop(
    result: PopResult?,
    navigationContext: NavigationContext = NoOpNavigationContext,
  ) {}

  /**
   * Called when the [InterceptingNavigator] resets the back stack to [newRoot].
   *
   * This is not called if navigation was intercepted.
   *
   * @param peekBackStack The state of the back stack before the [resetRoot] operation.
   * @param newRoot The new root screen that replaces the entire back stack.
   * @param options State options to apply when resetting the root.
   * @see InterceptingNavigator.resetRoot
   */
  public fun resetRoot(
    newRoot: Screen,
    options: StateOptions,
    navigationContext: NavigationContext = NoOpNavigationContext,
  ) {}
}

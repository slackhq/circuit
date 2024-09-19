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
}

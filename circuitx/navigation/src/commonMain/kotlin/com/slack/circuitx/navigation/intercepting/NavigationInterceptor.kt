// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A custom navigation processor for use with [InterceptingNavigator].
 *
 * @see InterceptingNavigator
 */
public interface NavigationInterceptor {

  /**
   * Navigates to the [screen], returning a [InterceptedResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun goTo(
    screen: Screen,
    navigationContext: NavigationContext = NoOpNavigationContext,
  ): InterceptedResult = Skipped

  /**
   * Navigates back in the back stack, returning a [InterceptedResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun pop(
    result: PopResult?,
    navigationContext: NavigationContext = NoOpNavigationContext,
  ): InterceptedResult = Skipped

  /**
   * Moves forward in navigation history, returning a [InterceptedResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun forward(
    navigationContext: NavigationContext = NoOpNavigationContext
  ): InterceptedResult = Skipped

  /**
   * Moves backward in navigation history, returning a [InterceptedResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun backward(
    navigationContext: NavigationContext = NoOpNavigationContext
  ): InterceptedResult = Skipped

  /**
   * Resets the back stack to the [newRoot], returning a [InterceptedResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun resetRoot(
    newRoot: Screen,
    options: StateOptions,
    navigationContext: NavigationContext = NoOpNavigationContext,
  ): InterceptedResult = Skipped

  public companion object {
    /**
     * Shorthand for a [InterceptedResult.Skipped] interceptor result where the interceptor did not
     * intercept the navigation.
     */
    public val Skipped: InterceptedResult = InterceptedResult.Skipped
    /**
     * Shorthand for a [InterceptedResult.Success] interceptor result where the interceptor has
     * consumed the navigation.
     */
    public val SuccessConsumed: InterceptedResult = InterceptedResult.Success(consumed = true)
  }
}

/**
 * The result of the [NavigationInterceptor] intercepting navigation operations.
 *
 * Common base for all intercepted navigation results.
 */
public sealed interface InterceptedResult {

  /** The [NavigationInterceptor] intercepted and rewrote the navigation destination. */
  public data class Rewrite(val navEvent: NavEvent) : InterceptedResult

  /** The [NavigationInterceptor] did not intercept the interaction. */
  public data object Skipped : InterceptedResult

  /**
   * The [NavigationInterceptor] interaction was successful.
   *
   * @param consumed If the [NavigationInterceptor] consumed the interaction.
   */
  public data class Success(val consumed: Boolean) : InterceptedResult

  /**
   * The [NavigationInterceptor] interaction was unsuccessful.
   *
   * @param consumed If the [NavigationInterceptor] consumed the interaction.
   */
  public data class Failure(val consumed: Boolean, val reason: Throwable? = null) :
    InterceptedResult

  public companion object {
    public fun Rewrite(screen: Screen): InterceptedResult = Rewrite(NavEvent.GoTo(screen))
  }
}

public typealias InterceptedGoToResult = InterceptedResult

public typealias InterceptedPopResult = InterceptedResult

public typealias InterceptedResetRootResult = InterceptedResult

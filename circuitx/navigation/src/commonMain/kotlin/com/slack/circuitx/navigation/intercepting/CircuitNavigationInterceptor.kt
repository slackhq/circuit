// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/**
 * A custom navigation processor for use with [CircuitInterceptingNavigator].
 *
 * @see CircuitInterceptingNavigator
 */
public interface CircuitNavigationInterceptor {

  /**
   * Navigates to the [screen], returning a [InterceptorGoToResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun goTo(screen: Screen): InterceptorGoToResult = Skipped

  /**
   * Navigates back looking at the [peekBackStack], returning a [InterceptorPopResult] for the
   * navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun pop(peekBackStack: ImmutableList<Screen>, result: PopResult?): InterceptorPopResult =
    Skipped

  /**
   * Resets the back stack to the [newRoot], returning a [InterceptorResetRootResult] for the
   * navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): InterceptorResetRootResult = Skipped

  public companion object {
    /**
     * Shorthand for a [InterceptorResult.Skipped] interceptor result where the interceptor did not
     * intercept the navigation.
     */
    public val Skipped: InterceptorResult = InterceptorResult.Skipped
    /**
     * Shorthand for a [InterceptorResult.Success] interceptor result where the interceptor has
     * consumed the navigation.
     */
    public val ConsumedSuccess: InterceptorResult = InterceptorResult.Success(consumed = true)
  }
}

/** The result of [CircuitNavigationInterceptor.goTo] being intercepted. */
public sealed interface InterceptorGoToResult {
  /** The [CircuitNavigationInterceptor] intercepted and rewrote the navigation destination. */
  public data class Rewrite(val screen: Screen) : InterceptorGoToResult
}

/** The result of [CircuitNavigationInterceptor.resetRoot] being intercepted. */
public sealed interface InterceptorResetRootResult {
  /** The [CircuitNavigationInterceptor] intercepted and rewrote the new root screen. */
  public data class Rewrite(val screen: Screen, val saveState: Boolean, val restoreState: Boolean) :
    InterceptorResetRootResult
}

/** The result of [CircuitNavigationInterceptor.pop] being intercepted. */
public sealed interface InterceptorPopResult

/** The result of the [CircuitNavigationInterceptor] intercepting [goTo] or [pop]. */
public sealed interface InterceptorResult :
  InterceptorGoToResult, InterceptorPopResult, InterceptorResetRootResult {

  /** The [CircuitNavigationInterceptor] did not intercept the interaction. */
  public data object Skipped : InterceptorResult

  /**
   * The [CircuitNavigationInterceptor] interaction was successful.
   *
   * @param consumed If the [CircuitNavigationInterceptor] consumed the interaction.
   */
  public data class Success(val consumed: Boolean) : InterceptorResult

  /**
   * The [CircuitNavigationInterceptor] interaction was unsuccessful.
   *
   * @param consumed If the [CircuitNavigationInterceptor] consumed the interaction.
   */
  public data class Failure(val consumed: Boolean, val reason: Throwable? = null) :
    InterceptorResult
}

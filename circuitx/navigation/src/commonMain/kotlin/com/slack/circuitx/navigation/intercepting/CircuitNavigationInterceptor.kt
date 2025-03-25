// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.foundation.NavEvent
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
   * Navigates to the [screen], returning a [Result] for the navigation.
   *
   * By default this will skip the navigation and return [Skipped].
   */
  public fun goTo(screen: Screen): Result = Skipped

  /**
   * Navigates back looking at the [peekBackStack], returning a [Result] for the navigation.
   *
   * By default this will skip the navigation and return [Skipped].
   */
  public fun pop(peekBackStack: ImmutableList<Screen>, result: PopResult?): Result = Skipped

  /** The result of the [CircuitNavigationInterceptor] intercepting [goTo] or [pop]. */
  public sealed interface Result {

    /** The [CircuitNavigationInterceptor] did not intercept the interaction. */
    public data object Skipped : Result

    /** The [CircuitNavigationInterceptor] intercepted and rewrote the navigation destination. */
    public data class Rewrite(val navEvent: NavEvent) : Result

    /**
     * The [CircuitNavigationInterceptor] interaction was successful.
     *
     * @param consumed If the [CircuitNavigationInterceptor] consumed the interaction.
     */
    public data class Success(val consumed: Boolean) : Result

    /**
     * The [CircuitNavigationInterceptor] interaction was unsuccessful.
     *
     * @param consumed If the [CircuitNavigationInterceptor] consumed the interaction.
     */
    public data class Failure(val consumed: Boolean, val reason: Throwable? = null) : Result
  }

  public companion object {
    /**
     * Shorthand for a [Result.Skipped] interceptor result where the interceptor did not intercept
     * the navigation.
     */
    public val Skipped: Result = Result.Skipped
    /**
     * Shorthand for a [Result.Success] interceptor result where the interceptor has consumed the
     * navigation.
     */
    public val ConsumedSuccess: Result = Result.Success(consumed = true)
  }
}

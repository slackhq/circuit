// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/**
 * A custom navigation processor for use with [InterceptingNavigator].
 *
 * @see InterceptingNavigator
 */
public interface NavigationInterceptor {

  /**
   * Navigates to the [screen], returning a [InterceptedGoToResult] for the navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun goTo(screen: Screen): InterceptedGoToResult = Skipped

  /**
   * Navigates back looking at the [peekBackStack], returning a [InterceptedPopResult] for the
   * navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun pop(peekBackStack: ImmutableList<Screen>, result: PopResult?): InterceptedPopResult =
    Skipped

  /**
   * Resets the back stack to the [newRoot], returning a [InterceptedResetRootResult] for the
   * navigation.
   *
   * By default this will skip intercepting the navigation and return [Skipped].
   */
  public fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): InterceptedResetRootResult = Skipped

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

/** The result of [NavigationInterceptor.goTo] being intercepted. */
public sealed interface InterceptedGoToResult {
  /** The [NavigationInterceptor] intercepted and rewrote the navigation destination. */
  public data class Rewrite(val screen: Screen) : InterceptedGoToResult
}

/** The result of [NavigationInterceptor.resetRoot] being intercepted. */
public sealed interface InterceptedResetRootResult {
  /** The [NavigationInterceptor] intercepted and rewrote the new root screen. */
  public data class Rewrite(val screen: Screen, val saveState: Boolean, val restoreState: Boolean) :
    InterceptedResetRootResult
}

/** The result of [NavigationInterceptor.pop] being intercepted. */
public sealed interface InterceptedPopResult

/** The result of the [NavigationInterceptor] intercepting [goTo] or [pop]. */
public sealed interface InterceptedResult :
  InterceptedGoToResult, InterceptedPopResult, InterceptedResetRootResult {

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
}

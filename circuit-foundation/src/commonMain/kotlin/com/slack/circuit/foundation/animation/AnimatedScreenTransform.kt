// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.screen.Screen

/**
 * Defines the animated transitions fo a [Screen] based on particular [AnimatedNavEvent]s.
 *
 * This interface allows you to customize the [EnterTransition], z-index, and [SizeTransform] for a
 * [Screen] as it becomes the top screen or is replaced by another [Screen] in the navigation stack.
 * You can also customize the [ExitTransition] for a screen that is no longer the top screen.
 *
 * Each of these can be customized based on the [AnimatedNavEvent] that triggered the transition.
 *
 * ### Example
 *
 * ```kotlin
 * object CustomScreenAnimatedTransform : AnimatedScreenTransform {
 *
 *   override fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
 *     animatedNavEvent: AnimatedNavEvent
 *   ): EnterTransition? {
 *     // Coming from `HomeScreen` we override the transition to slide in horizontally.
 *     return if (initialState.screen is HomeScreen) slideInHorizontally() else null
 *   }
 *
 *   override fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
 *     animatedNavEvent: AnimatedNavEvent
 *   ): ExitTransition? {
 *     // Going to `HomeScreen` we override the transition fade out.
 *     return if (targetState.screen is HomeScreen) fadeOut() else null
 *   }
 * }
 * ```
 */
@ExperimentalCircuitApi
public interface AnimatedScreenTransform {

  /**
   * Defines the transition used when this screen is becoming the top screen.
   *
   * @param animatedNavEvent The [AnimatedNavEvent] that triggered this transition.
   * @return An [EnterTransition] or null to use the default enter transition.
   * @see ContentTransform.targetContentEnter
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
    animatedNavEvent: AnimatedNavEvent
  ): EnterTransition? = null

  /**
   * Defines the transition used when this screen is no longer the top screen.
   *
   * @param animatedNavEvent The [AnimatedNavEvent] that triggered this transition.
   * @return An [ExitTransition] or null to use the default exit transition.
   * @see ContentTransform.initialContentExit
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
    animatedNavEvent: AnimatedNavEvent
  ): ExitTransition? = null

  /**
   * Defines the [ContentTransform.targetContentZIndex] of the screen when it is becoming the top
   * screen.
   *
   * @param animatedNavEvent The [AnimatedNavEvent] that triggered this transition.
   * @return The z-index or null to use the default z-index.
   * @see ContentTransform.targetContentZIndex
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.zIndex(
    animatedNavEvent: AnimatedNavEvent
  ): Float? = null // todo non-null?

  /**
   * Defines the [ContentTransform.sizeTransform] to use when this screen is becoming the top
   * screen.
   *
   * @param animatedNavEvent The [AnimatedNavEvent] that triggered this transition.
   * @return A [SizeTransform] or null to use the default size transform.
   * @see ContentTransform.sizeTransform
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.sizeTransform(
    animatedNavEvent: AnimatedNavEvent
  ): SizeTransform? = null
}

/** A default [AnimatedScreenTransform] that does nothing. */
@ExperimentalCircuitApi public object NoOpAnimatedScreenTransform : AnimatedScreenTransform

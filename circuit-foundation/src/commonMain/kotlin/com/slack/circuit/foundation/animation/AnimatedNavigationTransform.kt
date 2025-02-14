// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Transition
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.animation.AnimatedNavigationTransform.NavigationEvent

/**
 * [AnimatedNavigationTransform] allows you to define custom transitions between navigation states
 * in an [AnimatedNavDecoration]. You can use this to define unique transitions for specific
 * navigation events or states.
 *
 * ## Usage
 *
 * #### Transition Spec
 * - Override the `transitionSpec` method to define your custom transition logic.
 * - This method receives a [AnimatedNavigationTransform.NavigationEvent] indicating the type of
 *   navigation event that is occurring (`GoTo`, `Pop`, `RootReset`).
 * - It also has access to the current [AnimatedContentTransitionScope], from which you can get the
 *   `initialState` and `targetState` of the transition, as well as use the helper functions, like
 *   `slideIntoContainer`.
 * - Return a `ContentTransform` if you want to override the default transition for the given
 *   `NavigationEvent` and state. Return `null` if you want to use the `AnimatedNavDecorator`'s
 *   `defaultTransform` instead.
 *
 * #### Integrate in to Circuit
 * - When creating an `Circuit` instance provide your custom [AnimatedNavigationTransform] instances
 *   to [Circuit.Builder.addAnimatedNavigationTransform]
 * - These transforms will be checked in order to override the default [ContentTransform] from the
 *   [AnimatedNavDecorator].
 *
 * #### Transition Selection
 * - When a transition is happening, the [AnimatedNavDecoration] will compare each of the available
 *   [AnimatedNavigationTransform]s and select the first one that is able to handle the current
 *   transition. If no `AnimatedNavigationTransform` is able to handle the transition, then the
 *   default transition provided by the [AnimatedNavDecorator.defaultTransform] will be used.
 *
 * ### Example
 *
 * ```kotlin
 * class CustomTransform : AnimatedNavigationTransform {
 *
 *   override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
 *     navigationEvent: NavigationEvent
 *   ): ContentTransform? {
 *     return when (navigationEvent) {
 *       NavigationEvent.GoTo ->
 *         slideIntoContainer(towards = Right) togetherWith slideOutOfContainer(towards = Left)
 *       NavigationEvent.Pop ->
 *         fadeIn() togetherWith fadeOut()
 *       else -> null // Use default for RootReset
 *     }
 *   }
 * }
 * ```
 *
 * In this example, when the user navigates to a new screen (`GoTo`), the screen slides in towards
 * the right. When the user navigates back (`Pop`), the screen cross fades. For `RootReset`, no
 * custom transition is used (it falls back to the `defaultTransform`).
 *
 * @see AnimatedNavDecorator
 * @see AnimatedNavDecoration
 */
public interface AnimatedNavigationTransform {

  /**
   * Based on the source state, target state, and navigation event, return a [ContentTransform] to
   * override the transition spec in the [AnimatedNavDecoration] [AnimatedContent].
   *
   * Used as a way to provide a custom [ContentTransform] for specific navigation state changes
   * while using [AnimatedNavDecorator.defaultTransform] as the overall default.
   *
   * @return The [ContentTransform] to use or null if no override is desired.
   * @receiver Requires a [AnimatedContentTransitionScope] to access the [Transition.currentState]
   *   and [Transition.targetState]. Allows for the use of [AnimatedContentTransitionScope] methods
   *   (ex [AnimatedContentTransitionScope.using]) while building the returned [ContentTransform].
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    navigationEvent: NavigationEvent
  ): ContentTransform?

  public enum class NavigationEvent {
    /** Going to the [Transition.targetState] from the [Transition.currentState] */
    GoTo,
    /** Popping from the [Transition.currentState] back to to the [Transition.targetState] */
    Pop,
    /** The back stack has been reset to the [Transition.targetState]. */
    RootReset,
  }
}

/**
 * A non-nullable implementation of [AnimatedNavigationTransform]. Is the default [ContentTransform]
 * provided by a [AnimatedNavDecorator] to the [AnimatedNavDecoration].
 */
public interface RequiredAnimatedNavigationTransform : AnimatedNavigationTransform {
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    navigationEvent: NavigationEvent
  ): ContentTransform
}

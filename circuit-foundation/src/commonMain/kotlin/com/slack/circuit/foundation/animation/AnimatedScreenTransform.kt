// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform

/**
 * todo Docs
 *
 * This is registered to a particular screen via CircuitInject.
 */
public interface AnimatedScreenTransform {

  /** Transition used when this screen is becoming the top screen, based on [navigationEvent]. */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
    navigationEvent: NavigationEvent
  ): EnterTransition? = null

  /** Transition used when this screen is no longer the top screen, based on [navigationEvent]. */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
    navigationEvent: NavigationEvent
  ): ExitTransition? = null

  /** Z index of the screen when it is becoming the top screen, based on [navigationEvent]. */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.zIndex(
    navigationEvent: NavigationEvent
  ): Float? = null // todo non-null

  /**
   * Size transform used when this screen is becoming the top screen, based on [navigationEvent].
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.sizeTransform(
    navigationEvent: NavigationEvent
  ): SizeTransform? = null
}

public object NoOpAnimatedScreenTransform : AnimatedScreenTransform

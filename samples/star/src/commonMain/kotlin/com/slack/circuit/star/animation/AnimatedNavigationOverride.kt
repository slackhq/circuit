// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.foundation.animation.NavigationEvent
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.petdetail.PetDetailScreen

// @CircuitInject(PetDetailScreen::class, AppScope::class)
object PetDetailAnimatedNavigationOverride : AnimatedScreenTransform {

  override fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
    navigationEvent: NavigationEvent
  ): EnterTransition? {
    // Going to the detail screen
    if (initialState.screen !is HomeScreen) return null
    if (!targetState.screen.isSharedElementDetailScreen()) return null
    return EnterTransition.None
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
    navigationEvent: NavigationEvent
  ): ExitTransition? {
    // Going back to the home screen
    if (targetState.screen !is HomeScreen) return null
    if (!initialState.screen.isSharedElementDetailScreen()) return null
    return ExitTransition.None
  }
}

// @CircuitInject(HomeScreen::class, AppScope::class)
object HomeAnimatedNavigationOverride : AnimatedScreenTransform {

  override fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
    navigationEvent: NavigationEvent
  ): EnterTransition? {
    // Coming from the detail screen with shared elements
    return if (initialState.screen.isSharedElementDetailScreen()) EnterTransition.None else null
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
    navigationEvent: NavigationEvent
  ): ExitTransition? {
    // Going to the detail screen with shared elements
    return if (targetState.screen.isSharedElementDetailScreen()) ExitTransition.None else null
  }
}

private fun Screen.isSharedElementDetailScreen(): Boolean =
  this is PetDetailScreen && this.animal != null

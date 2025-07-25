// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.petdetail.PetDetailScreen

// todo @CircuitInject(PetDetailScreen::class, AppScope::class)
@ExperimentalCircuitApi
object PetDetailAnimatedScreenTransform : AnimatedScreenTransform {

  override fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
    animatedNavEvent: AnimatedNavEvent
  ): EnterTransition? {
    // Going to the detail screen
    if (initialState.top.screen !is HomeScreen) return null
    if (!targetState.top.screen.isSharedElementDetailScreen()) return null
    return EnterTransition.None
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
    animatedNavEvent: AnimatedNavEvent
  ): ExitTransition? {
    // Going back to the home screen
    if (targetState.top.screen !is HomeScreen) return null
    if (!initialState.top.screen.isSharedElementDetailScreen()) return null
    return ExitTransition.None
  }
}

// todo @CircuitInject(HomeScreen::class, AppScope::class)
@ExperimentalCircuitApi
object HomeAnimatedScreenTransform : AnimatedScreenTransform {

  override fun AnimatedContentTransitionScope<AnimatedNavState>.enterTransition(
    animatedNavEvent: AnimatedNavEvent
  ): EnterTransition? {
    // Coming from the detail screen with shared elements
    return if (initialState.top.screen.isSharedElementDetailScreen()) EnterTransition.None else null
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.exitTransition(
    animatedNavEvent: AnimatedNavEvent
  ): ExitTransition? {
    // Going to the detail screen with shared elements
    return if (targetState.top.screen.isSharedElementDetailScreen()) ExitTransition.None else null
  }
}

private fun Screen.isSharedElementDetailScreen(): Boolean =
  this is PetDetailScreen && this.animal != null

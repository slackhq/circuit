// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.animation.AnimatedNavigationTransform
import com.slack.circuit.foundation.animation.AnimatedNavigationTransform.NavigationEvent
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.petdetail.PetDetailScreen

object PetDetailAnimatedNavigationOverride : AnimatedNavigationTransform {

  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    navigationEvent: NavigationEvent
  ): ContentTransform? {
    return when {
      navigationEvent == NavigationEvent.GoTo &&
        initialState.screen is HomeScreen &&
        targetState.screen.isSharedElementDetailScreen() -> {
        // We're expecting shared elements to exist between the source and target so remove the
        // default animations.
        EnterTransition.None togetherWith ExitTransition.None
      }
      else -> null
    }
  }
}

// Split from PetDetailAnimatedNavigationOverride to verify AnimatedNavigationOverride lookups
object HomeAnimatedNavigationOverride : AnimatedNavigationTransform {
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    navigationEvent: NavigationEvent
  ): ContentTransform? {
    return when {
      navigationEvent == NavigationEvent.Pop &&
        initialState.screen.isSharedElementDetailScreen() &&
        targetState.screen is HomeScreen -> {
        // We're expecting shared elements to exist between the source and target so remove the
        // default animations.
        EnterTransition.None togetherWith ExitTransition.None
      }
      else -> null
    }
  }
}

private fun Screen.isSharedElementDetailScreen(): Boolean =
  this is PetDetailScreen && this.animal != null

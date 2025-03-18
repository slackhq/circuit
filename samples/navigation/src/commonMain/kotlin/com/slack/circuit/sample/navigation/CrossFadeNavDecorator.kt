// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.NavigatorDefaults.DefaultDecorator.DefaultAnimatedState
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState

data class CrossFadeNavDecoratorFactory(val durationMillis: Int = 300) :
  AnimatedNavDecorator.Factory {
  override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> =
    CrossFadeNavDecorator(durationMillis)
}

class CrossFadeNavDecorator<T : NavArgument>(private val durationMillis: Int) :
  AnimatedNavDecorator<T, DefaultAnimatedState<T>> by NavigatorDefaults.DefaultDecorator<T>() {

  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return fadeIn(tween(durationMillis)) togetherWith fadeOut(tween(durationMillis))
  }
}

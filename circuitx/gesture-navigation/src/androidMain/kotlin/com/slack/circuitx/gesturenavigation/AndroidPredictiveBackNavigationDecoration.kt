// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import android.os.Build
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import kotlin.math.absoluteValue

public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory
): AnimatedNavDecorator.Factory {
  return when {
    Build.VERSION.SDK_INT >= 34 -> AndroidPredictiveNavDecorator.Factory
    else -> fallback
  }
}

internal class AndroidPredictiveNavDecorator<T : NavArgument>(
  onBackInvoked: () -> Unit,
  onForwardInvoked: () -> Unit,
) : PredictiveNavigationDecorator<T>(onBackInvoked, onForwardInvoked) {

  // Track popped zIndex so screens are layered correctly
  private var zIndexDepth = 0f

  @OptIn(InternalCircuitApi::class)
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return when (animatedNavEvent) {
      // Adding to back stack
      AnimatedNavEvent.Forward,
      AnimatedNavEvent.GoTo -> {
        if (showForward) {
          // Handle all the animation in draggable
          EnterTransition.None togetherWith ExitTransition.None
        } else {
          NavigatorDefaults.forward
        }
      }
      // come back from back stack
      AnimatedNavEvent.Backward,
      AnimatedNavEvent.Pop -> {
        if (showBackward) {
            // Handle all the animation in predictiveBackMotion
            EnterTransition.None togetherWith ExitTransition.None
          } else {
            NavigatorDefaults.backward
          }
          .apply { targetContentZIndex = --zIndexDepth }
      }
      // Root reset. Crossfade
      AnimatedNavEvent.RootReset -> {
        zIndexDepth = 0f
        fadeIn() togetherWith fadeOut()
      }
    }
  }

  @Composable
  override fun AnimatedContentScope.Decoration(
    targetState: GestureNavTransitionHolder<T>,
    innerContent: @Composable (T) -> Unit,
  ) {
    Box(
      Modifier.predictiveBackMotion(
        enabled = { showBackward },
        isSeeking = { isSeeking },
        shape = MaterialTheme.shapes.extraLarge,
        elevation = 6.dp,
        transition = transition,
        offset = { swipeOffset },
        progress = { seekableTransitionState.fraction },
      )
    ) {
      innerContent(targetState.navStack.current)
    }
  }

  object Factory : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(navigator: Navigator): AnimatedNavDecorator<T, *> {
      return AndroidPredictiveNavDecorator(
        onBackInvoked = {
          // todo Handle root?
          navigator.backward()
        },
        onForwardInvoked = { navigator.forward() },
      )
    }
  }
}

private val DecelerateEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

/**
 * Implements most of the treatment specified at
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
 */
private fun Modifier.predictiveBackMotion(
  enabled: () -> Boolean,
  isSeeking: () -> Boolean,
  shape: Shape,
  elevation: Dp,
  transition: Transition<EnterExitState>,
  progress: () -> Float,
  offset: () -> Offset,
): Modifier = graphicsLayer {
  val p = progress()
  val o = offset()
  // If we're at progress 0f, skip setting any parameters
  if (!enabled() || p == 0f || !o.isValid()) return@graphicsLayer

  sharedElementTransition(isSeeking, shape, elevation, transition, p, o)
}

// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#shared-element-transition
private fun GraphicsLayerScope.sharedElementTransition(
  isSeeking: () -> Boolean,
  shape: Shape,
  elevation: Dp,
  transition: Transition<EnterExitState>,
  progress: Float,
  offset: Offset,
) {
  // Only animate the element that is exiting.
  when (transition.targetState) {
    EnterExitState.PreEnter,
    EnterExitState.Visible -> return
    EnterExitState.PostExit -> Unit
  }

  clip = true
  this.shape = shape
  shadowElevation = elevation.toPx()

  val scale = lerp(1f, 0.9f, progress.absoluteValue)
  scaleX = scale
  scaleY = scale

  // Ramp margin from 0.dp to 8.dp as it becomes available.
  val marginX = ((size.width * (1 - scale)) / 2).coerceAtMost(8.dp.toPx())
  val marginY = ((size.height * (1 - scale)) / 2).coerceAtMost(8.dp.toPx())
  val maxTranslationX = (progress.absoluteValue * (size.width / 20))
  // Determine a y axis easing to match the x progress
  val progressY = (offset.y.absoluteValue / size.height).coerceIn(0f, 1f)
  val transformY = DecelerateEasing.transform(progressY)
  val maxTranslationY = (transformY * (size.height / 20))

  translationX =
    offset.x.coerceIn(
      (-maxTranslationX + marginX).coerceAtMost(0f),
      (maxTranslationX - marginX).coerceAtLeast(0f),
    )
  translationY =
    offset.y.coerceIn(
      (-maxTranslationY + marginY).coerceAtMost(0f),
      (maxTranslationY - marginY).coerceAtLeast(0f),
    )

  if (!isSeeking()) {
    alpha = lerp(1f, 0f, progress.absoluteValue)
  }
}

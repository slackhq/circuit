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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.math.absoluteValue

private val FastOutExtraSlowInEasing = CubicBezierEasing(0.208333f, 0.82f, 0.25f, 1f)
private val AccelerateEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private val DecelerateEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

private const val DEBUG_MULTIPLIER = 1
private const val SHORT_DURATION = 83 * DEBUG_MULTIPLIER
private const val NORMAL_DURATION = 450 * DEBUG_MULTIPLIER

public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory {
  return when {
    Build.VERSION.SDK_INT >= 34 -> AndroidPredictiveBackNavDecorator.Factory(onBackInvoked)
    else -> fallback
  }
}

internal class AndroidPredictiveBackNavDecorator<T : NavArgument>(onBackInvoked: () -> Unit) :
  PredictiveBackNavigationDecorator<T>(onBackInvoked) {

  // Track popped zIndex so screens are layered correctly
  private var zIndexDepth = 0f

  @OptIn(InternalCircuitApi::class)
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return when (animatedNavEvent) {
      // adding to back stack
      AnimatedNavEvent.Forward,
      AnimatedNavEvent.GoTo -> {
        NavigatorDefaults.forward
      }
      // come back from back stack
      AnimatedNavEvent.Backward,
      AnimatedNavEvent.Pop -> {
        // Handle all the animation in predictiveBackMotion
        (EnterTransition.None togetherWith ExitTransition.None).apply {
          targetContentZIndex = --zIndexDepth
        }
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
    var fullWidth by remember { mutableIntStateOf(0) }
    val fade by transition.fade()
    val offset by transition.offset { fullWidth }
    Box(
      Modifier.layout { measurable, constraints ->
          val placeable = measurable.measure(constraints)
          val size = constraints.constrain(IntSize(placeable.width, placeable.height))
          fullWidth = size.width
          layout(size.width, size.height) { placeable.place(offset.x, offset.y) }
        }
        .graphicsLayer { alpha = fade }
        .predictiveBackMotion(
          enabled = { showPrevious },
          isSeeking = { isSwipeInProgress },
          shape = MaterialTheme.shapes.extraLarge,
          elevation = if (SharedElementTransitionScope.isTransitionActive) 0.dp else 6.dp,
          transition = transition,
          offset = { swipeOffset },
          progress = { seekableTransitionState.fraction },
        )
    ) {
      innerContent(targetState.navStack.active)
    }
  }

  /**
   * Fade the same as [androidx.compose.animation.fadeIn] + [androidx.compose.animation.fadeOut]
   * from [NavigatorDefaults.backward].
   */
  @Composable
  private fun Transition<EnterExitState>.fade(): State<Float> {
    return animateFloat(
      transitionSpec = {
        when {
          EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
            tween(durationMillis = SHORT_DURATION, delayMillis = 0, easing = LinearEasing)

          EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
            tween(durationMillis = SHORT_DURATION, delayMillis = 50, easing = AccelerateEasing)

          else -> spring()
        }
      }
    ) { targetState ->
      if (isSwipeInProgress) {
        1f
      } else {
        when (targetState) {
          EnterExitState.Visible -> 1f
          EnterExitState.PreEnter -> 1f
          EnterExitState.PostExit -> 0f
        }
      }
    }
  }

  /**
   * Offset the same as
   * [androidx.compose.animation.slideInHorizontally] + [androidx.compose.animation.slideOutHorizontally]
   * from [NavigatorDefaults.backward].
   */
  @Composable
  private fun Transition<EnterExitState>.offset(fullWidth: () -> Int): State<IntOffset> {
    return animateIntOffset(
      transitionSpec = {
        tween(durationMillis = NORMAL_DURATION, easing = FastOutExtraSlowInEasing)
      }
    ) { targetState ->
      val preEnter = fullWidth() / -10
      val postExit = fullWidth() / 10
      if (isSwipeInProgress) {
        IntOffset.Zero
      } else {
        when (targetState) {
          EnterExitState.Visible -> IntOffset.Zero
          EnterExitState.PreEnter -> IntOffset(preEnter, 0)
          EnterExitState.PostExit -> IntOffset(postExit, 0)
        }
      }
    }
  }

  class Factory(private val onBackInvoked: () -> Unit) : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
      return AndroidPredictiveBackNavDecorator(onBackInvoked = onBackInvoked)
    }
  }
}

/**
 * Implements most of the treatment specified at
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
 */
private fun Modifier.predictiveBackMotion(
  enabled: () -> Boolean,
  isSeeking: () -> Boolean,
  shape: CornerBasedShape,
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
  shape: CornerBasedShape,
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

  val shapeElevationFraction = (progress.absoluteValue * 5f).coerceAtMost(1f)
  this.shape =
    RoundedCornerShape(
      topStart = (shape.topStart.toPx(size, this) * shapeElevationFraction).toDp(),
      topEnd = (shape.topEnd.toPx(size, this) * shapeElevationFraction).toDp(),
      bottomEnd = (shape.bottomEnd.toPx(size, this) * shapeElevationFraction).toDp(),
      bottomStart = (shape.bottomStart.toPx(size, this) * shapeElevationFraction).toDp(),
    )
  shadowElevation = lerp(0f, elevation.toPx(), shapeElevationFraction)

  val scale = lerp(1f, 0.9f, progress.absoluteValue)
  scaleX = scale
  scaleY = scale

  // Ramp margin from 0.dp to 8.dp as it becomes available.
  val marginX = ((size.width * (1 - scale)) / 2).coerceAtMost(8.dp.toPx())
  val marginY = ((size.height * (1 - scale)) / 2).coerceAtMost(8.dp.toPx())
  val maxTranslationX = (progress.absoluteValue * (size.width / 20))
  // Determine a y-axis easing to match the x progress
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

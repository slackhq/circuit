// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory,
  onBackInvoked: () -> Unit,
): AnimatedNavDecorator.Factory {
  return when {
    Build.VERSION.SDK_INT >= 34 -> AndroidPredictiveBackNavDecorator.Factory(onBackInvoked)
    else -> fallback
  }
}

@Suppress("SlotReused") // This is an advanced use case
@RequiresApi(34)
internal class AndroidPredictiveBackNavDecorator<T : NavArgument>(
  private val onBackInvoked: () -> Unit
) : AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  private lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>

  private var showPrevious by mutableStateOf(false)
  private var isSeeking by mutableStateOf(false)
  private var swipeProgress by mutableFloatStateOf(0f)
  private var swipeOffset by mutableStateOf(Offset.Zero)

  // Track popped zIndex so screens are layered correctly
  private var zIndexDepth = 0f

  override fun targetState(
    args: ImmutableList<T>,
    backStackDepth: Int,
  ): GestureNavTransitionHolder<T> {
    return GestureNavTransitionHolder(args.first(), backStackDepth, args.last())
  }

  @Composable
  override fun updateTransition(
    args: ImmutableList<T>,
    backStackDepth: Int,
  ): Transition<GestureNavTransitionHolder<T>> {
    val scope = rememberStableCoroutineScope()
    val current = remember(args) { targetState(args, backStackDepth) }
    val previous =
      remember(args) {
        if (args.size > 1) {
          targetState(args.subList(1, args.size), backStackDepth - 1)
        } else null
      }

    seekableTransitionState = remember { SeekableTransitionState(current) }

    LaunchedEffect(current) {
      swipeProgress = 0f
      isSeeking = false
      seekableTransitionState.animateTo(current)
      // After the current state has changed (i.e. any transition has completed),
      // clear out any transient state
      showPrevious = false
      swipeOffset = Offset.Zero
    }

    LaunchedEffect(previous, current) {
      if (previous != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = previous)
            }
          }
      }
    }

    if (backStackDepth > 1) {
      BackHandler(
        onBackProgress = { progress, offset ->
          showPrevious = progress != 0f
          swipeProgress = progress
          swipeOffset = offset
        },
        onBackCancelled = {
          scope.launch {
            isSeeking = false
            seekableTransitionState.snapTo(current)
          }
        },
        onBackInvoked = { onBackInvoked() },
      )
    }
    return rememberTransition(seekableTransitionState, label = "AndroidPredictiveBackNavDecorator")
  }

  @OptIn(InternalCircuitApi::class)
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return when (animatedNavEvent) {
      // adding to back stack
      AnimatedNavEvent.GoTo -> {
        NavigatorDefaults.forward
      }
      // come back from back stack
      AnimatedNavEvent.Pop -> {
        if (showPrevious) {
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
        enabled = { showPrevious },
        isSeeking = { isSeeking },
        shape = MaterialTheme.shapes.extraLarge,
        elevation = if (SharedElementTransitionScope.isTransitionActive) 0.dp else 6.dp,
        transition = transition,
        offset = { swipeOffset },
        progress = { seekableTransitionState.fraction },
      )
    ) {
      innerContent(targetState.record)
    }
  }

  class Factory(private val onBackInvoked: () -> Unit) : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
      return AndroidPredictiveBackNavDecorator(onBackInvoked = onBackInvoked)
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

@RequiresApi(34)
@Composable
private fun BackHandler(
  onBackProgress: (Float, Offset) -> Unit,
  onBackCancelled: () -> Unit,
  animatedEnabled: Boolean = true,
  onBackInvoked: () -> Unit,
) {
  val onBackDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("OnBackPressedDispatcher is not available")
  val lastAnimatedEnabled by rememberUpdatedState(animatedEnabled)
  val lastOnBackProgress by rememberUpdatedState(onBackProgress)
  val lastOnBackCancelled by rememberUpdatedState(onBackCancelled)
  val lastOnBackInvoked by rememberUpdatedState(onBackInvoked)

  DisposableEffect(onBackDispatcher) {
    val callback =
      object : OnBackPressedCallback(true) {

        var initialTouch = Offset.Zero

        override fun handleOnBackStarted(backEvent: BackEventCompat) {
          if (lastAnimatedEnabled) {
            initialTouch = Offset(backEvent.touchX, backEvent.touchY)
            lastOnBackProgress(0f, Offset.Zero)
          }
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
          if (lastAnimatedEnabled) {
            lastOnBackProgress(
              when (backEvent.swipeEdge) {
                BackEvent.EDGE_LEFT -> backEvent.progress
                else -> -backEvent.progress
              },
              Offset(backEvent.touchX, backEvent.touchY) - initialTouch,
            )
          }
        }

        override fun handleOnBackCancelled() {
          initialTouch = Offset.Zero
          if (lastAnimatedEnabled) {
            lastOnBackCancelled()
          }
        }

        override fun handleOnBackPressed() {
          lastOnBackInvoked()
          initialTouch = Offset.Zero
        }
      }

    onBackDispatcher.addCallback(callback)

    onDispose { callback.remove() }
  }
}

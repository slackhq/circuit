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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.animation.AnimatedNavigationTransform.NavigationEvent
import com.slack.circuit.foundation.animation.RequiredAnimatedNavigationTransform
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
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
  private var swipeProgress by mutableFloatStateOf(0f)

  private var backStackDepthState by mutableIntStateOf(0)

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

    backStackDepthState = backStackDepth
    seekableTransitionState = remember { SeekableTransitionState(current) }
    val transition = rememberTransition(seekableTransitionState, label = "GestureNavDecoration")

    LaunchedEffect(current) {
      // When the current state has changed (i.e. any transition has completed),
      // clear out any transient state
      showPrevious = false
      swipeProgress = 0f
      seekableTransitionState.animateTo(current)
    }

    LaunchedEffect(previous, current) {
      if (previous != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
              seekableTransitionState.seekTo(fraction = progress, targetState = previous)
            }
          }
      }
    }

    if (backStackDepth > 1) {
      BackHandler(
        onBackProgress = { progress ->
          showPrevious = progress != 0f
          swipeProgress = progress
        },
        onBackCancelled = { scope.launch { seekableTransitionState.snapTo(current) } },
        onBackInvoked = { onBackInvoked() },
      )
    }
    return transition
  }

  @OptIn(InternalCircuitApi::class)
  override val defaultTransform: RequiredAnimatedNavigationTransform =
    object : RequiredAnimatedNavigationTransform {
      override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
        navigationEvent: NavigationEvent
      ): ContentTransform {
        return when (navigationEvent) {
          // adding to back stack
          NavigationEvent.GoTo -> NavigatorDefaults.DefaultDecoration.forward
          // come back from back stack
          NavigationEvent.Pop -> {
            if (showPrevious) {
                EnterTransition.None togetherWith scaleOut(targetScale = 0.8f) + fadeOut()
              } else {
                NavigatorDefaults.DefaultDecoration.backward
              }
              .apply { targetContentZIndex = -1f }
          }
          // Root reset. Crossfade
          NavigationEvent.RootReset -> fadeIn() togetherWith fadeOut()
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
        shape = MaterialTheme.shapes.extraLarge,
        elevation = if (SharedElementTransitionScope.isTransitionActive) 0.dp else 6.dp,
        progress = {
          if (swipeProgress != 0f && seekableTransitionState.currentState == targetState) {
            swipeProgress
          } else 0f
        },
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

/**
 * Implements most of the treatment specified at
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#designing-gesture
 *
 * The only piece missing is the vertical shift.
 */
private fun Modifier.predictiveBackMotion(
  shape: Shape,
  elevation: Dp,
  progress: () -> Float,
): Modifier = graphicsLayer {
  val p = progress()
  // If we're at progress 0f, skip setting any parameters
  if (p == 0f) return@graphicsLayer

  translationX = -(8.dp * p).toPx()
  shadowElevation = elevation.toPx()

  val scale = lerp(1f, 0.9f, p.absoluteValue)
  scaleX = scale
  scaleY = scale
  transformOrigin = TransformOrigin(pivotFractionX = if (p > 0) 1f else 0f, pivotFractionY = 0.5f)

  this.shape = shape
  clip = true
}

@RequiresApi(34)
@Composable
private fun BackHandler(
  onBackProgress: (Float) -> Unit,
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

        override fun handleOnBackStarted(backEvent: BackEventCompat) {
          if (lastAnimatedEnabled) {
            lastOnBackProgress(0f)
          }
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
          if (lastAnimatedEnabled) {
            lastOnBackProgress(
              when (backEvent.swipeEdge) {
                BackEvent.EDGE_LEFT -> backEvent.progress
                else -> -backEvent.progress
              }
            )
          }
        }

        override fun handleOnBackCancelled() {
          if (lastAnimatedEnabled) {
            lastOnBackCancelled()
          }
        }

        override fun handleOnBackPressed() = lastOnBackInvoked()
      }

    onBackDispatcher.addCallback(callback)

    onDispose { callback.remove() }
  }
}

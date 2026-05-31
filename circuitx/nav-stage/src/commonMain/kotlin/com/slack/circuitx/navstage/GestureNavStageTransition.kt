// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.math.abs
import kotlinx.coroutines.CancellationException

/**
 * A [NavStageTransition] that drives predictive back gestures with Material motion.
 *
 * During the gesture, the current stage scales down and translates in the swipe direction while the
 * previous stage is shown behind it. Builds the previous state from the navigation stack using
 * [SeekableTransitionState] so the animation can be scrubbed interactively. Calls [onBack] when the
 * gesture completes.
 */
@ExperimentalNavStageApi
public class GestureNavStageTransition(private val onBack: () -> Unit) : NavStageTransition {

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  ) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember { mutableStateOf(Offset.Zero) }
    var isSwipeInProgress by remember { mutableStateOf(false) }
    var showPrevious by remember { mutableStateOf(false) }

    val previous =
      remember(targetState) {
        val args = targetState.args
        val hasBackward = args.backwardItems.iterator().hasNext()
        if (hasBackward) {
          val forward = listOf(args.active) + args.forwardItems
          val current = args.backwardItems.first()
          val backward = args.backwardItems.drop(1)
          NavStageTransitionState(
            stageKey = targetState.stageKey,
            args = navStackListOf(forward, current, backward),
          )
        } else null
      }

    val seekableTransitionState = remember { SeekableTransitionState(targetState) }

    LaunchedEffect(targetState) {
      swipeProgress = 0f
      swipeOffset = Offset.Zero
      isSwipeInProgress = false
      showPrevious = false
      seekableTransitionState.animateTo(targetState)
    }

    LaunchedEffect(previous, targetState) {
      if (previous != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
              isSwipeInProgress = true
              try {
                seekableTransitionState.seekTo(fraction = abs(progress), targetState = previous)
              } catch (_: CancellationException) {}
            }
          }
      }
    }

    PredictiveBackEventHandler(
      isEnabled = previous != null,
      onBackProgress = { progress, offset ->
        showPrevious = progress != 0f
        swipeProgress = progress
        swipeOffset = offset
      },
      onBackCancelled = {
        swipeProgress = 0f
        swipeOffset = Offset.Zero
        isSwipeInProgress = false
        seekableTransitionState.animateTo(targetState)
        showPrevious = false
      },
      onBackCompleted = { onBack() },
    )

    rememberTransition(seekableTransitionState, label = "GestureNavStageTransition")

    Box(Modifier.fillMaxSize()) {
      if (showPrevious && previous != null) {
        // Mark the previous state as secondary so overlapping items render as
        // shared-bounds placeholders instead of real content (avoids movableContent crash).
        CompositionLocalProvider(LocalNavStagePrimary provides false) {
          content(previous)
        }
      }

      Box(
        Modifier.fillMaxSize().graphicsLayer {
          if (!showPrevious) return@graphicsLayer
          val progress = abs(swipeProgress)
          if (progress == 0f) return@graphicsLayer

          val scale = 1f - (progress * 0.1f)
          scaleX = scale
          scaleY = scale

          val maxTranslationX = progress * (size.width / 20)
          val maxTranslationY = progress * (size.height / 20)
          translationX =
            swipeOffset.x.coerceIn(
              -maxTranslationX.coerceAtMost(0f),
              maxTranslationX.coerceAtLeast(0f),
            )
          translationY =
            swipeOffset.y.coerceIn(
              -maxTranslationY.coerceAtMost(0f),
              maxTranslationY.coerceAtLeast(0f),
            )

          if (!isSwipeInProgress) {
            alpha = 1f - progress
          }
        }
      ) {
        content(targetState)
      }
    }
  }
}

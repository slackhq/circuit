// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.math.abs

/**
 * A [NavStageTransition] that drives predictive back gestures with Material motion.
 *
 * During the gesture the current stage scales down and translates in the swipe direction. The stage
 * for the previous stack is shown behind it, unless the current stage already renders that stack's
 * active record, in which case there is nothing to reveal. Calls [onBack] when the gesture
 * completes.
 *
 * This drives the gesture directly rather than through Circuit's `AnimatedNavDecoration`, so its
 * Material treatment applies on every platform, not just Android.
 */
@ExperimentalNavStageApi
public class GestureNavStageTransition(private val onBack: () -> Unit) : NavStageTransition {

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  ) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember { mutableStateOf(Offset.Zero) }
    var showPrevious by remember { mutableStateOf(false) }

    val previousArgs = remember(targetState.args) { previousArgsOf(targetState.args) }
    // Resolved through stateFor so the previous stack gets the stage it needs, not this one's.
    val previousState = if (previousArgs != null) stateFor(previousArgs) else null

    // A record can only be composed once at a time. If the current stage already shows the record
    // we would reveal behind it, compose only the current stage.
    val previousToReveal =
      previousState?.takeIf { state ->
        targetState.visibleItems.none { it.key == state.args.active.key }
      }

    LaunchedEffect(targetState) {
      swipeProgress = 0f
      swipeOffset = Offset.Zero
      showPrevious = false
    }

    PredictiveBackEventHandler(
      isEnabled = previousState != null,
      onBackProgress = { progress, offset ->
        showPrevious = progress != 0f
        swipeProgress = progress
        swipeOffset = offset
      },
      onBackCancelled = {
        swipeProgress = 0f
        swipeOffset = Offset.Zero
        showPrevious = false
      },
      onBackCompleted = { onBack() },
    )

    Box(Modifier.fillMaxSize()) {
      if (showPrevious && previousToReveal != null) {
        content(previousToReveal)
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
          translationX = swipeOffset.x.coerceIn(-maxTranslationX, maxTranslationX)
          translationY = swipeOffset.y.coerceIn(-maxTranslationY, maxTranslationY)
        }
      ) {
        content(targetState)
      }
    }
  }
}

/** The stack as it was one step back, or null if there is nothing behind the active item. */
private fun <T : NavArgument> previousArgsOf(args: NavStackList<T>): NavStackList<T>? {
  val backward = args.backwardItems.iterator()
  if (!backward.hasNext()) return null
  return navStackListOf(
    listOf(args.active) + args.forwardItems,
    args.backwardItems.first(),
    args.backwardItems.drop(1),
  )
}

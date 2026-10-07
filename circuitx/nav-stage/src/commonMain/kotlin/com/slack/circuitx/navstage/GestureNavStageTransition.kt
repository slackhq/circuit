// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.math.abs
import kotlinx.coroutines.CancellationException

/**
 * A [NavStageTransition] that drives predictive back gestures with Material motion.
 *
 * During the gesture the current stage scales down and translates in the swipe direction while the
 * stage for the previous stack is shown behind it. Records the current stage already shows render
 * as shared-bounds placeholders in the previous stage. The gesture seeks a
 * [SeekableTransitionState] so shared elements track it. Calls [onBack] when the gesture completes,
 * or [Navigator.pop] if [onBack] is null.
 *
 * This drives the gesture directly rather than through Circuit's `AnimatedNavDecoration`, so its
 * Material treatment applies on every platform, not just Android.
 */
@ExperimentalNavStageApi
public class GestureNavStageTransition(private val onBack: (() -> Unit)? = null) :
  NavStageTransition {

  @OptIn(
    InternalCircuitApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalTransitionApi::class,
  )
  @Composable
  override fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
    navigator: Navigator,
    @Suppress("SlotReused") content: @Composable (NavStageTransitionState<T>) -> Unit,
  ) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var swipeOffset by remember { mutableStateOf(Offset.Zero) }
    var showPrevious by remember { mutableStateOf(false) }

    val previousArgs = remember(targetState.args) { previousArgsOf(targetState.args) }
    // Resolved through stateFor so the previous stack gets the stage it needs, not this one's.
    val previous = if (previousArgs != null) stateFor(previousArgs) else null

    val seekableTransitionState = remember { SeekableTransitionState(targetState) }

    LaunchedEffect(targetState) {
      swipeProgress = 0f
      swipeOffset = Offset.Zero
      showPrevious = false
      seekableTransitionState.animateTo(targetState)
    }

    LaunchedEffect(previous, targetState) {
      if (previous != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
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
        seekableTransitionState.animateTo(targetState)
        showPrevious = false
      },
      onBackCompleted = {
        if (onBack != null) {
          onBack()
        } else {
          navigator.pop()
        }
      },
    )

    val transition =
      rememberTransition(seekableTransitionState, label = "GestureNavStageTransition")

    val previousScope = transition.animatedVisibilityScope { previous != null && it == previous }
    val targetScope = transition.animatedVisibilityScope { it == targetState }

    Box(Modifier.fillMaxSize()) {
      if (showPrevious && previous != null) {
        CompositionLocalProvider(LocalNavStagePrimary provides false) {
          ProvideAnimatedTransitionScope(Navigation, previousScope) { content(previous) }
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
          translationX = swipeOffset.x.coerceIn(-maxTranslationX, maxTranslationX)
          translationY = swipeOffset.y.coerceIn(-maxTranslationY, maxTranslationY)
        }
      ) {
        ProvideAnimatedTransitionScope(Navigation, targetScope) { content(targetState) }
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

@OptIn(ExperimentalTransitionApi::class)
@Composable
private fun <T> Transition<T>.animatedVisibilityScope(
  visible: (T) -> Boolean
): AnimatedVisibilityScope {
  val childTransition =
    createChildTransition(label = "GestureNavStageTransition child") { state ->
      targetEnterExit(visible, state)
    }
  return remember(childTransition) { SimpleAnimatedVisibilityScope(childTransition) }
}

private data class SimpleAnimatedVisibilityScope(
  override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope

@Composable
private fun <T> Transition<T>.targetEnterExit(
  visible: (T) -> Boolean,
  targetState: T,
): EnterExitState =
  key(this) {
    if (this.isSeeking) {
      if (visible(targetState)) {
        EnterExitState.Visible
      } else {
        if (visible(this.currentState)) {
          EnterExitState.PostExit
        } else {
          EnterExitState.PreEnter
        }
      }
    } else {
      val hasBeenVisible = remember { mutableStateOf(false) }
      if (visible(currentState)) {
        hasBeenVisible.value = true
      }
      if (visible(targetState)) {
        EnterExitState.Visible
      } else {
        if (hasBeenVisible.value) {
          EnterExitState.PostExit
        } else {
          EnterExitState.PreEnter
        }
      }
    }
  }

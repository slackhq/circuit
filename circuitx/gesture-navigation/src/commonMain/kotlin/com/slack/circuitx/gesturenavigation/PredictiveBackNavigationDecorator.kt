// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.geometry.Offset
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal abstract class PredictiveBackNavigationDecorator<T : NavArgument>(
  private val onBackInvoked: () -> Unit
) : AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  protected lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>
    private set

  protected var showPrevious: Boolean by mutableStateOf(false)
    private set

  protected var isSeeking: Boolean by mutableStateOf(false)
    private set

  protected var swipeProgress: Float by mutableFloatStateOf(0f)
    private set

  protected var swipeOffset: Offset by mutableStateOf(Offset.Zero)
    private set

  override fun targetState(args: ImmutableList<T>): GestureNavTransitionHolder<T> {
    return GestureNavTransitionHolder(args)
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @Composable
  override fun updateTransition(args: ImmutableList<T>): Transition<GestureNavTransitionHolder<T>> {
    val scope = rememberStableCoroutineScope()
    val current = remember(args) { targetState(args) }
    val previous =
      remember(args) {
        if (args.size > 1) {
          targetState(args.subList(1, args.size))
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

    if (args.size > 1) {
      BackHandler(
        onBackProgress = { progress, offset ->
          showPrevious = progress != 0f
          swipeProgress = progress
          swipeOffset = offset
        },
        onBackCancelled = {
          scope.launch {
            isSeeking = false
            seekableTransitionState.animateTo(current)
          }
        },
        onBackInvoked = { onBackInvoked() },
      )
    }
    return rememberTransition(seekableTransitionState, label = "PredictiveBackNavigationDecorator")
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BackHandler(
  onBackProgress: (Float, Offset) -> Unit,
  onBackCancelled: () -> Unit,
  onBackInvoked: () -> Unit,
) {
  val lastOnBackProgress by rememberUpdatedState(onBackProgress)
  val lastOnBackCancelled by rememberUpdatedState(onBackCancelled)
  val lastOnBackInvoked by rememberUpdatedState(onBackInvoked)

  PredictiveBackHandler(
    enabled = true,
    onBack = { progress ->
      try {
        var initialTouch = Offset.Zero
        progress.collect { backEvent ->
          if (initialTouch == Offset.Zero) {
            initialTouch = Offset(backEvent.touchX, backEvent.touchY)
            lastOnBackProgress(0f, Offset.Zero)
          } else {
            lastOnBackProgress(
              when (backEvent.swipeEdge) {
                0 -> backEvent.progress // BackEventCompat.EDGE_LEFT
                else -> -backEvent.progress
              },
              Offset(backEvent.touchX, backEvent.touchY) - initialTouch,
            )
          }
        }
        lastOnBackInvoked()
      } catch (_: CancellationException) {
        lastOnBackCancelled()
      }
    },
  )
}

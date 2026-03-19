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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.math.abs

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

  override fun targetState(args: NavStackList<T>): GestureNavTransitionHolder<T> {
    return GestureNavTransitionHolder(args)
  }

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun updateTransition(args: NavStackList<T>): Transition<GestureNavTransitionHolder<T>> {
    val current = remember(args) { targetState(args) }
    val previous =
      remember(args) {
        val hasBackward = args.backwardItems.iterator().hasNext()
        if (hasBackward) {
          // Building the state we'd go to if we go backwards.
          val forward = listOf(args.active) + args.forwardItems
          val current = args.backwardItems.first()
          val backward = args.backwardItems.drop(1)
          targetState(navStackListOf(forward, current, backward))
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
    PredictiveBackEventHandler(
      isEnabled = previous != null,
      onBackProgress = { progress, offset ->
        showPrevious = progress != 0f
        swipeProgress = progress
        swipeOffset = offset
      },
      onBackCancelled = {
        isSeeking = false
        seekableTransitionState.animateTo(current)
      },
      onBackCompleted = { onBackInvoked() },
    )
    return rememberTransition(seekableTransitionState, label = "PredictiveBackNavigationDecorator")
  }
}

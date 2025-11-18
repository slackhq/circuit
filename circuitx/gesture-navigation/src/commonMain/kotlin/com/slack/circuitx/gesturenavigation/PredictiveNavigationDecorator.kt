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
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.internal.PredictiveNavDirection
import com.slack.circuit.foundation.internal.PredictiveNavEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.navStackListOf
import kotlin.math.abs

public abstract class PredictiveNavigationDecorator<T : NavArgument>(
  private val onBackInvoked: () -> Unit,
  private val onForwardInvoked: () -> Unit,
) : AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  protected lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>
    private set

  protected var showBackward: Boolean by mutableStateOf(false)
  protected var showForward: Boolean by mutableStateOf(false)
  protected var swipeProgress: Float by mutableFloatStateOf(0f)

  protected var isSeeking: Boolean by mutableStateOf(false)
    private set

  protected var swipeOffset: Offset by mutableStateOf(Offset.Zero)
    private set

  override fun targetState(args: NavStackList<T>): GestureNavTransitionHolder<T> {
    return GestureNavTransitionHolder(args)
  }

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun updateTransition(args: NavStackList<T>): Transition<GestureNavTransitionHolder<T>> {
    val currentState = remember(args) { targetState(args) }
    val backwardState =
      remember(args) {
        val hasBackward = args.backward.iterator().hasNext()
        if (hasBackward) {
          val forward = listOf(args.current) + args.forward
          val current = args.backward.first()
          val backward = args.backward.drop(1)
          targetState(navStackListOf(forward, current, backward))
        } else null
      }
    val forwardState =
      remember(args) {
        val hasForward = args.forward.iterator().hasNext()
        if (hasForward) {
          val forward = args.forward.drop(1)
          val current = args.forward.first()
          val backward = listOf(args.current) + args.backward
          targetState(navStackListOf(forward, current, backward))
        } else null
      }
    seekableTransitionState = remember { SeekableTransitionState(currentState) }

    LaunchedEffect(currentState) {
      swipeProgress = 0f
      isSeeking = false
      seekableTransitionState.animateTo(currentState)
      // After the current state has changed (i.e. any transition has completed),
      // clear out any transient state
      showBackward = false
      showForward = false
      swipeOffset = Offset.Zero
    }

    // todo Based on the swipe direction
    LaunchedEffect(backwardState, currentState, forwardState) {
      snapshotFlow { Triple(swipeProgress, showForward, showBackward) }
        .collect { (progress, showingForward, showingBackward) ->
          val showingBackward =
            backwardState != null && showingBackward && !showingForward && progress != 0f
          val showingForward =
            forwardState != null && showingForward && !showingBackward && progress != 0f
          when {
            showingBackward -> {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = backwardState)
            }
            showingForward -> {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = forwardState)
            }
          }
        }
    }

    PredictiveNavEventHandler(
      isBackEnabled = backwardState != null,
      isForwardEnabled = forwardState != null,
      onProgress = { direction, progress, offset ->
        when (direction) {
          PredictiveNavDirection.Back -> {
            showForward = false
            showBackward = progress != 0f
            swipeProgress = progress
            swipeOffset = offset
          }
          PredictiveNavDirection.Forward -> {
            showBackward = false
            showForward = progress != 0f
            swipeProgress = progress
            swipeOffset = offset
          }
        }
      },
      onCancelled = {
        isSeeking = false
        seekableTransitionState.animateTo(currentState)
      },
      onCompleted = { direction ->
        when (direction) {
          PredictiveNavDirection.Back -> onBackInvoked()
          PredictiveNavDirection.Forward -> onForwardInvoked()
        }
      },
    )
    return rememberTransition(seekableTransitionState, label = "PredictiveNavigationDecorator")
  }
}

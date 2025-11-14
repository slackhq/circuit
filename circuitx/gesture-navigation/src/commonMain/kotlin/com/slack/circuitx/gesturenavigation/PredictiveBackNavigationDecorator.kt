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
import com.slack.circuit.foundation.NavStackList
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.internal.PredictiveNavDirection
import com.slack.circuit.foundation.internal.PredictiveNavEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import kotlin.math.abs

public abstract class PredictiveBackNavigationDecorator<T : NavArgument>(
  private val onBackInvoked: () -> Unit,
  private val onForwardInvoked: () -> Unit,
) : AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  protected lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>
    private set

  protected var showPrevious: Boolean by mutableStateOf(false)
  protected var showNext: Boolean by mutableStateOf(false)
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
    val current = remember(args) { targetState(args) }
    val previous =
      remember(args) {
        val backwardStack = args.backwardStack()
        if (backwardStack.size > 1) {
          targetState(NavStackList(backwardStack.subList(1, backwardStack.size)))
        } else null
      }

    val next =
      remember(args) {
        val forwardStack = args.forwardStack()
        if (forwardStack.isNotEmpty()) {
          targetState(NavStackList(forwardStack.subList(0, forwardStack.size)))
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
      showNext = false
      swipeOffset = Offset.Zero
    }

    // todo Based on the swipe direction
    LaunchedEffect(previous, current, next) {
      snapshotFlow { Triple(swipeProgress, showNext, showPrevious) }
        .collect { (progress, showingNext, showingPrevious) ->
          val showingPrevious =
            previous != null && showingPrevious && !showingNext && progress != 0f
          val showingNext = next != null && showingNext && !showingPrevious && progress != 0f
          when {
            showingPrevious -> {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = previous)
            }
            showingNext -> {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = next)
            }
          }
        }
    }

    PredictiveNavEventHandler(
      isBackEnabled = previous != null,
      isForwardEnabled = next != null,
      onProgress = { direction, progress, offset ->
        when (direction) {
          PredictiveNavDirection.Back -> {
            showNext = false
            showPrevious = progress != 0f
            swipeProgress = progress
            swipeOffset = offset
          }
          PredictiveNavDirection.Forward -> {
            showPrevious = false
            showNext = progress != 0f
            swipeProgress = progress
            swipeOffset = offset
          }
        }
      },
      onCancelled = {
        isSeeking = false
        seekableTransitionState.animateTo(current)
      },
      onCompleted = { direction ->
        when (direction) {
          PredictiveNavDirection.Back -> onBackInvoked()
          PredictiveNavDirection.Forward -> onForwardInvoked()
        }
      },
    )
    return rememberTransition(seekableTransitionState, label = "PredictiveBackNavigationDecorator")
  }
}

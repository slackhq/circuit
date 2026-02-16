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
import com.slack.circuit.foundation.scene.AnimatedScene
import com.slack.circuit.foundation.scene.AnimatedSceneTransitionDriver
import com.slack.circuit.foundation.scene.AnimatedSceneTransitionScope
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.math.abs

internal abstract class PredictiveBackNavigationDecorator<T : NavArgument>(
  onBackInvoked: () -> Unit
) : AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  protected val driver = PredictiveBackAnimatedTransitionDriver(onBackInvoked)
  protected lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>
    private set

  override fun targetState(args: NavStackList<T>): GestureNavTransitionHolder<T> {
    return GestureNavTransitionHolder(args)
  }

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun updateTransition(args: NavStackList<T>): Transition<GestureNavTransitionHolder<T>> {
    seekableTransitionState = remember { SeekableTransitionState(targetState(args)) }
    driver.UpdateTransition(seekableTransitionState, args, targetState = { targetState(it) })
    return rememberTransition(seekableTransitionState, label = "PredictiveBackNavigationDecorator")
  }
}

public class PredictiveBackAnimatedSceneTransitionDriver(onBackInvoked: () -> Unit) :
  AnimatedSceneTransitionDriver {
  private val driver = PredictiveBackAnimatedTransitionDriver(onBackInvoked)

  @Composable
  override fun <T : NavArgument, S : AnimatedScene> AnimatedSceneTransitionScope<S>
    .AnimateTransition(args: NavStackList<out T>, targetScene: (NavStackList<out T>) -> S) {
    driver.UpdateTransition(transitionState, args, targetScene)
  }
}

internal class PredictiveBackAnimatedTransitionDriver(private val onBackInvoked: () -> Unit) {

  var showPrevious: Boolean by mutableStateOf(false)
    private set

  var isSeeking: Boolean by mutableStateOf(false)
    private set

  var swipeProgress: Float by mutableFloatStateOf(0f)
    private set

  var swipeOffset: Offset by mutableStateOf(Offset.Zero)
    private set

  @OptIn(InternalCircuitApi::class)
  @Composable
  fun <T : NavArgument, S> UpdateTransition(
    state: SeekableTransitionState<S>,
    args: NavStackList<T>,
    targetState: (NavStackList<T>) -> S,
  ) {
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

    LaunchedEffect(state.currentState) {
      swipeProgress = 0f
      isSeeking = false
      state.animateTo(current)
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
              state.seekTo(fraction = abs(progress), targetState = previous)
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
        state.animateTo(current)
      },
      onBackCompleted = { onBackInvoked() },
    )
  }
}

// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import kotlin.math.abs

@ExperimentalNavStageApi
public class GestureNavStageTransition(
  private val onBack: () -> Unit,
) : NavStageTransition {

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  ) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var isSwipeInProgress by remember { mutableStateOf(false) }

    val hasPrevious = targetState.args.backwardItems.iterator().hasNext()

    PredictiveBackEventHandler(
      isEnabled = hasPrevious,
      onBackProgress = { progress, _ ->
        swipeProgress = progress
        isSwipeInProgress = true
      },
      onBackCancelled = {
        swipeProgress = 0f
        isSwipeInProgress = false
      },
      onBackCompleted = {
        swipeProgress = 0f
        isSwipeInProgress = false
        onBack()
      },
    )

    val scale = if (isSwipeInProgress) 1f - (abs(swipeProgress) * 0.1f) else 1f

    Box(
      Modifier.fillMaxSize().graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
    ) {
      content(targetState)
    }
  }
}

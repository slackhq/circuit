// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

@Stable
@ExperimentalNavStageApi
public fun interface NavStageTransition {
  @Composable
  public fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  )

  public companion object {
    public val None: NavStageTransition = NavStageTransition { targetState, content ->
      content(targetState)
    }

    public val Crossfade: NavStageTransition = NavStageTransition { targetState, content ->
      androidx.compose.animation.Crossfade(
        targetState = targetState,
        contentKey = { it.stageKey },
      ) { state ->
        content(state)
      }
    }
  }
}

@Immutable
@ExperimentalNavStageApi
public data class NavStageTransitionState<T : NavArgument>(
  val stageKey: Any,
  val args: NavStackList<T>,
)

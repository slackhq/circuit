// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Controls the animation when transitioning between different [NavStage] layouts.
 *
 * Stage transitions animate the outer boundary when the layout type changes (e.g. single-pane to
 * dual-pane). This is distinct from [PaneTransition] which animates individual items within a pane.
 */
@Stable
@ExperimentalNavStageApi
public interface NavStageTransition {
  @Composable
  public fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  )

  public companion object {
    public val None: NavStageTransition =
      object : NavStageTransition {
        @Composable
        override fun <T : NavArgument> AnimatedStageContent(
          targetState: NavStageTransitionState<T>,
          content: @Composable (NavStageTransitionState<T>) -> Unit,
        ) {
          content(targetState)
        }
      }

    public val Crossfade: NavStageTransition =
      object : NavStageTransition {
        @Composable
        override fun <T : NavArgument> AnimatedStageContent(
          targetState: NavStageTransitionState<T>,
          content: @Composable (NavStageTransitionState<T>) -> Unit,
        ) {
          Crossfade(targetState = targetState.stageKey) { content(targetState) }
        }
      }
  }
}

/**
 * Snapshot of the current stage layout and navigation stack, used as the target for stage
 * transitions.
 */
@Immutable
@ExperimentalNavStageApi
public data class NavStageTransitionState<T : NavArgument>(
  val stageKey: Any,
  val args: NavStackList<T>,
)

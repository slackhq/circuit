// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
  /**
   * Renders [targetState], optionally animating from another state.
   *
   * To render a stack other than the target's, build its state with [stateFor] rather than
   * constructing one directly: [stateFor] resolves the [NavStage] that stack actually needs, where
   * reusing the target's stage would render a layout against a stack it was never validated for.
   */
  @Composable
  public fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  )

  public companion object {
    public val None: NavStageTransition =
      object : NavStageTransition {
        @Composable
        override fun <T : NavArgument> AnimatedStageContent(
          targetState: NavStageTransitionState<T>,
          stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
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
          stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
          content: @Composable (NavStageTransitionState<T>) -> Unit,
        ) {
          // Keyed on the stage layout so only a layout change animates, and each slot renders its
          // own state rather than the incoming one.
          AnimatedContent(
            targetState = targetState,
            contentKey = { it.stageKey },
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
          ) { state ->
            content(state)
          }
        }
      }
  }
}

/**
 * Snapshot of the current stage layout and navigation stack, used as the target for stage
 * transitions.
 *
 * Obtain instances from [NavStageTransition.AnimatedStageContent]'s `stateFor` rather than
 * constructing them, so [visibleItems] stays consistent with the stage resolved for [args].
 */
@Immutable
@ExperimentalNavStageApi
public data class NavStageTransitionState<T : NavArgument>(
  val stageKey: Any,
  val args: NavStackList<T>,
  val visibleItems: List<T>,
)

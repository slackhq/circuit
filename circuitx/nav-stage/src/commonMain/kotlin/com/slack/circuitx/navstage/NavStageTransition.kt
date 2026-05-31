// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

/**
 * Controls the animation when transitioning between different [NavStage] layouts.
 *
 * Stage transitions animate the outer boundary when the layout type changes (e.g. single-pane to
 * dual-pane). This is distinct from [PaneTransition] which animates individual items within a pane.
 *
 * All built-in transitions automatically provide the [Navigation] `AnimatedVisibilityScope` so that
 * [NavStagePaneScope.Pane] calls can use shared element bounds to animate pane positions between
 * stage layouts.
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
    /** Instant swap with no animation. No shared element animation is applied. */
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

    /**
     * Crossfade between stage layouts using [AnimatedContent]. Provides the [Navigation]
     * `AnimatedVisibilityScope` so shared element bounds can animate pane positions during the
     * transition.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    public val Crossfade: NavStageTransition =
      object : NavStageTransition {
        @Composable
        override fun <T : NavArgument> AnimatedStageContent(
          targetState: NavStageTransitionState<T>,
          content: @Composable (NavStageTransitionState<T>) -> Unit,
        ) {
          AnimatedContent(
            targetState = targetState,
            contentKey = { it.stageKey },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "NavStageCrossfade",
          ) { state ->
            ProvideAnimatedTransitionScope(Navigation, this@AnimatedContent) { content(state) }
          }
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

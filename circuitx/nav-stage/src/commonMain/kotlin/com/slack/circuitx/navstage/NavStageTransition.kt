// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

/**
 * Controls the animation when transitioning between different [NavStage] layouts.
 *
 * Stage transitions animate the outer boundary when the layout type changes (e.g. single-pane to
 * dual-pane). This is distinct from [PaneTransition] which animates individual items within a pane.
 *
 * All built-in transitions provide the [Navigation] `AnimatedVisibilityScope` so that
 * [NavStagePaneScope.Pane] calls can use shared element bounds to animate pane positions between
 * stage layouts. This scope follows stage changes, not navigation within a pane. Content that
 * animates with in-pane navigation should use [findActiveStageScope] or the [Pane] scope.
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
    navigator: Navigator,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  )

  public companion object {
    /**
     * Instant swap with no animation. Still provides the [Navigation] `AnimatedVisibilityScope`, so
     * content that requires it keeps working.
     */
    public val None: NavStageTransition =
      object : NavStageTransition {
        @OptIn(ExperimentalSharedTransitionApi::class)
        @Composable
        override fun <T : NavArgument> AnimatedStageContent(
          targetState: NavStageTransitionState<T>,
          stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
          navigator: Navigator,
          content: @Composable (NavStageTransitionState<T>) -> Unit,
        ) {
          AnimatedContent(
            targetState = targetState,
            contentKey = { it.stageKey },
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
          ) { state ->
            ProvideAnimatedTransitionScope(Navigation, this@AnimatedContent) { content(state) }
          }
        }
      }

    /**
     * Crossfade between stage layouts using [AnimatedContent]. Provides the [Navigation]
     * `AnimatedVisibilityScope` so shared element bounds can animate pane positions during the
     * transition.
     */
    public val Crossfade: NavStageTransition =
      object : NavStageTransition {
        @OptIn(ExperimentalSharedTransitionApi::class)
        @Composable
        override fun <T : NavArgument> AnimatedStageContent(
          targetState: NavStageTransitionState<T>,
          stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
          navigator: Navigator,
          content: @Composable (NavStageTransitionState<T>) -> Unit,
        ) {
          // Keyed on the stage layout so only a layout change animates, and each slot renders its
          // own state rather than the incoming one.
          AnimatedContent(
            targetState = targetState,
            contentKey = { it.stageKey },
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
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
 *
 * Instances come from [NavStageTransition.AnimatedStageContent]: its `targetState`, or `stateFor`
 * for other stacks. Each carries the [NavStage] it was resolved with, so it always renders exactly
 * its [visibleItems] even if the stage for [args] would resolve differently now.
 */
@Immutable
@ExperimentalNavStageApi
public class NavStageTransitionState<T : NavArgument>
internal constructor(
  internal val stage: NavStage<T>,
  public val args: NavStackList<T>,
  public val visibleItems: List<T>,
) {
  public val stageKey: Any
    get() = stage.key

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NavStageTransitionState<*>) return false
    return stageKey == other.stageKey && args == other.args && visibleItems == other.visibleItems
  }

  override fun hashCode(): Int {
    var result = stageKey.hashCode()
    result = 31 * result + args.hashCode()
    result = 31 * result + visibleItems.hashCode()
    return result
  }

  override fun toString(): String =
    "NavStageTransitionState(stageKey=$stageKey, args=$args, visibleItems=$visibleItems)"
}

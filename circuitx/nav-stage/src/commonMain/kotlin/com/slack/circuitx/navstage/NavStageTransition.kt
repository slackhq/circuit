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
 * animates with in-pane navigation should use [findActiveStageScope] or [PaneAnimatedScope].
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
   *
   * Pass [targetState] itself to [content] for the slot showing the target, and to only one slot.
   * Records it shows move into that slot, and every other slot renders them as placeholders.
   * [navigator] routes calls through the stage's [NavStage.navigationPolicy], so pop through it.
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
 *
 * Compared by identity: the decoration composes the records of the `targetState` instance it passed
 * in, so render that instance, in one slot, rather than an equal copy.
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

  internal fun hasSameContent(other: NavStageTransitionState<*>): Boolean =
    stageKey == other.stageKey && args == other.args && visibleItems == other.visibleItems

  override fun toString(): String =
    "NavStageTransitionState(stageKey=$stageKey, args=$args, visibleItems=$visibleItems)"
}

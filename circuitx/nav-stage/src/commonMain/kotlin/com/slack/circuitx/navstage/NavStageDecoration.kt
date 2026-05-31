// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * A [NavDecoration] that delegates layout to a [NavStageStrategy]-resolved [NavStage].
 *
 * Composes a [NavStageTransition] around the stage content and an optional [NavStageFrame] around
 * everything. When the strategy returns `null`, falls back to [SinglePaneNavStage].
 */
@Stable
@ExperimentalNavStageApi
public class NavStageDecoration(
  private val strategies: List<NavStageStrategy>,
  private val stageTransition: NavStageTransition = NavStageTransition.None,
  private val frame: NavStageFrame = NavStageFrame.None,
) : NavDecoration {

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val stage =
      strategies.firstNotNullOfOrNull { it.calculateStage(args) }
        ?: SinglePaneNavStage.get()
    frame.Content(modifier, stage, args) { NavStageContent(stage, args, stageTransition, content) }
  }
}

/**
 * CompositionLocal indicating whether the current composition is the primary (target) state. Set to
 * `false` by transitions that overlay a secondary composition (e.g. [GestureNavStageTransition]
 * showing the previous state behind the current one).
 */
internal val LocalNavStagePrimary = compositionLocalOf { true }

@OptIn(ExperimentalNavStageApi::class)
@Composable
internal fun <T : NavArgument> NavStageContent(
  stage: NavStage<T>,
  args: NavStackList<T>,
  stageTransition: NavStageTransition,
  content: @Composable (T) -> Unit,
) {
  var previousArgs by remember { mutableStateOf(args) }
  val navEvent =
    remember(args) {
      determineNavEvent(previousArgs, args)
    }
  SideEffect {
    previousArgs = args
  }

  // Track the previous stage so we can render the outgoing layout during transitions.
  var previousStage by remember { mutableStateOf(stage) }
  val resolvedPreviousStage =
    remember(stage.key) {
      previousStage
    }
  SideEffect {
    previousStage = stage
  }

  // Pre-compute the items the target stage will render, so overlapping items in the
  // outgoing/secondary composition can be rendered as placeholders (with shared bounds).
  val targetRenderedItemKeys = remember(stage, args) { stage.renderedItemKeys(args) }

  val targetState = NavStageTransitionState(stageKey = stage.key, args = args)
  stageTransition.AnimatedStageContent(targetState) { state ->
    val isPrimary = LocalNavStagePrimary.current
    val isTargetState = state.stageKey == stage.key

    // Determine which stage layout to use for rendering.
    // The target state uses the current stage; any non-target state (outgoing during a
    // stage-to-stage transition) uses the previous stage so shared bounds are positioned
    // according to the old layout.
    val activeStage = if (isTargetState) stage else resolvedPreviousStage

    // Compute placeholder keys: items that are already composed by the primary/target state
    // should be rendered as empty shared-bounds placeholders in the secondary composition
    // to avoid composing a movableContentOf in multiple places simultaneously.
    val placeholderKeys =
      remember(isPrimary, isTargetState, activeStage, state.args, targetRenderedItemKeys) {
        if (isPrimary && isTargetState) {
          emptySet()
        } else {
          val activeItems = activeStage.renderedItemKeys(state.args)
          activeItems.intersect(targetRenderedItemKeys)
        }
      }

    val paneScope =
      NavStagePaneScopeImpl(
        content = content,
        navEvent = navEvent,
        placeholderItemKeys = placeholderKeys,
      )
    activeStage.Content(state.args, paneScope, Modifier)
  }
}

@OptIn(ExperimentalNavStageApi::class)
private fun <T : NavArgument> determineNavEvent(
  initial: NavStackList<T>,
  target: NavStackList<T>,
): PaneNavEvent {
  if (initial.root.key != target.root.key) return PaneNavEvent.RootReset

  val previous = initial.active
  val current = target.active

  if (previous.key == current.key) return PaneNavEvent.GoTo

  val initialBackStack = initial.backwardItems
  val initialForwardStack = initial.forwardItems
  val targetForwardStack = target.forwardItems

  return when {
    current in initialBackStack &&
      previous !in initialForwardStack &&
      previous in targetForwardStack -> PaneNavEvent.Backward

    current in initialBackStack && previous !in targetForwardStack -> PaneNavEvent.Pop
    current in initialForwardStack && current !in targetForwardStack -> PaneNavEvent.Forward
    else -> PaneNavEvent.GoTo
  }
}

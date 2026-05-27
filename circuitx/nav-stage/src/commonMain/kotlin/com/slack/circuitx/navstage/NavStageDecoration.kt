// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
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
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val stage =
      strategies.firstNotNullOfOrNull { it.calculateStage(args) }
        ?: remember { SinglePaneNavStage() }
    frame.Content(modifier, stage, args) { NavStageContent(stage, args, stageTransition, content) }
  }
}

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
      val event = determineNavEvent(previousArgs, args)
      previousArgs = args
      event
    }

  val targetState = NavStageTransitionState(stageKey = stage.key, args = args)
  stageTransition.AnimatedStageContent(targetState) { state ->
    val paneScope = NavStagePaneScopeImpl(content = content, navEvent = navEvent)
    stage.Content(state.args, paneScope, Modifier)
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

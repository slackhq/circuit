// Copyright (C) 2025 Slack Technologies, LLC
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

@Stable
@ExperimentalNavStageApi
public class NavStageDecoration(
  private val strategy: NavStageStrategy,
  private val stageTransition: NavStageTransition = NavStageTransition.None,
  private val frame: NavStageFrame = NavStageFrame.None,
) : NavDecoration {

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val stage = strategy.calculateStage(args) ?: SinglePaneNavStage()
    frame.Content(modifier, stage, args) {
      NavStageContent(stage, args, stageTransition, content)
    }
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
  val navEvent = remember(args) {
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
  previous: NavStackList<T>,
  current: NavStackList<T>,
): PaneNavEvent {
  if (previous.root.key != current.root.key) return PaneNavEvent.RootReset

  val previousActive = previous.active
  val currentActive = current.active

  if (previousActive.key == currentActive.key) return PaneNavEvent.GoTo

  val currentInPreviousBackward = current.backwardItems.any { it.key == previousActive.key }
  val previousInCurrentForward = current.forwardItems.any { it.key == previousActive.key }

  return when {
    previousInCurrentForward -> PaneNavEvent.Pop
    currentInPreviousBackward -> PaneNavEvent.Backward
    current.forwardItems.any { it.key == currentActive.key } -> PaneNavEvent.Forward
    else -> PaneNavEvent.GoTo
  }
}

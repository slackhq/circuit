// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.determineAnimatedNavEvent
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * A [NavDecoration] that delegates layout to a [NavStageStrategy]-resolved [NavStage].
 *
 * Composes a [NavStageTransition] around the stage content and an optional [NavStageFrame] around
 * everything. When no strategy matches, falls back to [SinglePaneNavStage].
 *
 * This is a sibling of `AnimatedNavDecoration` rather than an extension of it, so
 * `Circuit.Builder.addAnimatedScreenTransform` is not applied to stage content.
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
    val stage = rememberStage(strategies, args)
    frame.Content(modifier, stage, args) {
      NavStageContent(strategies, stage, args, stageTransition, navigator, content)
    }
  }
}

/**
 * Resolves the stage for [args]. Every strategy is invoked and the first non-null result wins, so
 * the composable call layout does not shift as strategies start and stop matching.
 */
@OptIn(ExperimentalNavStageApi::class)
@Composable
private fun <T : NavArgument> rememberStage(
  strategies: List<NavStageStrategy>,
  args: NavStackList<T>,
): NavStage<T> {
  val resolved =
    strategies
      .mapIndexed { index, strategy -> key(index) { strategy.calculateStage(args) } }
      .firstNotNullOfOrNull { it }
  return resolved ?: SinglePaneNavStage.get()
}

@OptIn(ExperimentalNavStageApi::class)
@Composable
internal fun <T : NavArgument> NavStageContent(
  strategies: List<NavStageStrategy>,
  stage: NavStage<T>,
  args: NavStackList<T>,
  stageTransition: NavStageTransition,
  navigator: Navigator,
  content: @Composable (T) -> Unit,
) {
  val navEvent = rememberNavEvent(args)
  val targetState = rememberTransitionState(stage, args)
  val targetItemKeys =
    remember(targetState) { targetState.visibleItems.mapTo(HashSet()) { it.key } }
  val composedRecords = remember { ComposedRecords() }

  stageTransition.AnimatedStageContent(
    targetState = targetState,
    stateFor = { stack -> rememberTransitionState(rememberStage(strategies, stack), stack) },
    navigator = navigator,
  ) { slotState ->
    val isPrimary = slotState == targetState
    val state = if (isPrimary) targetState else slotState
    val owner = remember { Any() }
    val paneScope =
      NavStagePaneScopeImpl(
        content = content,
        navEvent = navEvent,
        owner = owner,
        isPrimary = isPrimary,
        targetItemKeys = targetItemKeys,
        composedRecords = composedRecords,
      )
    state.stage.Content(state.args, paneScope, Modifier)
  }
}

@OptIn(ExperimentalNavStageApi::class)
@Composable
private fun <T : NavArgument> rememberTransitionState(
  stage: NavStage<T>,
  args: NavStackList<T>,
): NavStageTransitionState<T> =
  remember(stage, args) {
    val visibleItems = stage.visibleItems(args)
    // Two panes sharing a record fails deep inside the navigation host's state registry with a
    // message that names neither the stage nor the pane, so fail here where the stage is nameable.
    val seen = HashSet<Any>(visibleItems.size)
    visibleItems.forEach { item ->
      require(seen.add(item.key)) {
        "NavStage '${stage.key}' returned item key '${item.key}' twice from visibleItems. " +
          "Each pane must render a distinct record."
      }
    }
    NavStageTransitionState(stage, args, visibleItems)
  }

/** Classifies the navigation that produced [args]. */
@OptIn(InternalCircuitApi::class)
@Composable
private fun <T : NavArgument> rememberNavEvent(args: NavStackList<T>): AnimatedNavEvent {
  // Held outside snapshot state: writing observable state during composition would invalidate this
  // scope and force an extra recomposition on every navigation.
  val previousHolder = remember { PreviousArgsHolder<T>() }
  return remember(args) {
    val previous = previousHolder.args
    previousHolder.args = args
    if (previous == null) {
      AnimatedNavEvent.GoTo
    } else {
      determineAnimatedNavEvent(previous, args) ?: AnimatedNavEvent.GoTo
    }
  }
}

private class PreviousArgsHolder<T : NavArgument> {
  var args: NavStackList<T>? = null
}

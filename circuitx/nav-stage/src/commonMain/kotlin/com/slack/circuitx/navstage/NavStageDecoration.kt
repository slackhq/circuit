// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.determineAnimatedNavEvent
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

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
    ProvidePaneWindowInfo(modifier) {
      val stage = rememberStage(strategies, args)
      frame.Content(Modifier, stage, args) {
        NavStageContent(strategies, stage, args, stageTransition, navigator, content)
      }
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
  return resolved ?: SinglePaneNavStage()
}

@OptIn(ExperimentalNavStageApi::class, InternalCircuitApi::class)
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
  val paneNavigators = remember { PaneNavigators<T>() }
  paneNavigators.host = navigator
  paneNavigators.args = args
  SideEffect {
    paneNavigators.prune { key ->
      depthOf(args, key) >= 0 ||
        args.forwardItems.any { it.key == key } ||
        composedRecords.isClaimed(key)
    }
  }

  val policy = stage.navigationPolicy
  val stageNavigator = remember(paneNavigators) { StageNavigator(paneNavigators) }
  PredictiveBackEventHandler(
    isEnabled = policy !== NavStageNavigationPolicy.Passthrough && args.backwardItems.any(),
    onBackProgress = { _, _ -> },
    onBackCancelled = {},
    onBackCompleted = { stageNavigator.pop() },
  )

  stageTransition.AnimatedStageContent(
    targetState = targetState,
    stateFor = { stack -> rememberTransitionState(rememberStage(strategies, stack), stack) },
    navigator = stageNavigator,
  ) { state ->
    val isPrimary = state === targetState
    val itemKeys = remember(state) { state.visibleItems.mapTo(HashSet()) { it.key } }
    val paneScope =
      NavStagePaneScopeImpl(
        content = content,
        navEvent = navEvent,
        stageKey = state.stageKey,
        itemKeys = itemKeys,
        isPrimary = isPrimary,
        targetItemKeys = targetItemKeys,
        composedRecords = composedRecords,
        paneNavigators = paneNavigators,
        navigationPolicy = state.stage.navigationPolicy,
      )
    state.stage.Content(state.visibleItems, paneScope, Modifier)
  }
}

/** Routes calls through the active record's navigator, so they come from the top pane. */
private class StageNavigator<T : NavArgument>(private val navigators: PaneNavigators<T>) :
  Navigator {
  private val top: Navigator
    get() = navigators.args?.let { navigators.navigatorFor(it.active.key) } ?: navigators.host

  override fun goTo(screen: Screen): Boolean = top.goTo(screen)

  override fun pop(result: PopResult?): Screen? = top.pop(result)

  override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> =
    top.resetRoot(newRoot, options)

  override fun forward(): Boolean = top.forward()

  override fun backward(): Boolean = top.backward()

  override fun peek(): Screen? = navigators.host.peek()

  override fun peekBackStack(): List<Screen> = navigators.host.peekBackStack()

  override fun peekNavStack(): NavStackList<Screen>? = navigators.host.peekNavStack()
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

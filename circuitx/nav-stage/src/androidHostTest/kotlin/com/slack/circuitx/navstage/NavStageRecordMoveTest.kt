// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalNavStageApi::class, ExperimentalSharedTransitionApi::class)

package com.slack.circuitx.navstage

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 34, maxSdk = 36)
@RunWith(RobolectricTestRunner::class)
class NavStageRecordMoveTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val mounts = mutableMapOf<String, Int>()
  private val navigators = mutableMapOf<String, Navigator>()

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, navigator, _ ->
        val label = (screen as RScreen).label
        navigators[label] = navigator
        presenterOf { RState(label) }
      }
      .addUiFactory { _, _ ->
        ui<RState> { state, modifier ->
          remember { mounts.merge(state.label, 1, Int::plus) }
          BasicText(state.label, modifier.fillMaxSize().testTag(state.label))
        }
      }
      .build()

  @Test
  fun recordMovingToAnEarlierPaneKeepsItsState() {
    val navigator = setNavContent(listOf(RA, RS1), supportingPaneDecoration())
    composeTestRule.mainClock.autoAdvance = false
    navigator.goTo(RS2)
    advanceFramesComposingOnce(30)
    assertEquals(1, mounts["s1"])
    assertComposed("s1", "s2")
  }

  @Test
  fun recordMovingToALaterPaneKeepsItsState() {
    val navigator = setNavContent(listOf(RA, RS1, RS2), supportingPaneDecoration())
    composeTestRule.mainClock.autoAdvance = false
    navigator.pop()
    advanceFramesComposingOnce(30)
    assertEquals(1, mounts["s1"])
    assertComposed("a", "s1")
  }

  @Test
  fun recordMovingFromDetailToListPaneKeepsItsState() {
    val navigator = setNavContent(listOf(RA), listDetailDecoration())
    composeTestRule.mainClock.autoAdvance = false
    navigator.goTo(RS1)
    advanceFramesComposingOnce(30)
    navigator.goTo(RS2)
    advanceFramesComposingOnce(30)
    assertEquals(1, mounts["s1"])
    assertComposed("s1", "s2")
  }

  @Test
  fun committingPreviewSlotMovesItsRecordsWithoutRemounting() {
    val navigator = setNavContent(listOf(RA), NavStageDecoration(emptyList(), PreviewTransition))
    composeTestRule.mainClock.autoAdvance = false
    navigator.goTo(RS1)
    composeTestRule.mainClock.advanceTimeBy(2_000)
    PreviewFlag.on = true
    advanceFramesComposingOnce(5)
    val previewMounts = mounts["a"]
    navigator.pop()
    PreviewFlag.on = false
    advanceFramesComposingOnce(30)
    assertEquals(previewMounts, mounts["a"])
    assertComposed("a")
  }

  @Test
  fun goToFromListPaneReplacesDetail() {
    setNavContent(listOf(RA, RS1), listDetailDecoration())
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle { navigators.getValue("a").goTo(RS2) }
    composeTestRule.waitForIdle()
    assertEquals(listOf<Screen>(RS2, RA), navigators.getValue("a").peekBackStack())
  }

  private fun supportingPaneDecoration() =
    NavStageDecoration(
      listOf(
        object : NavStageStrategy {
          @Composable
          override fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>? {
            if (args.backwardItems.none()) return null
            return remember { SupportingPaneStage() }
          }
        }
      )
    )

  private fun listDetailDecoration() =
    NavStageDecoration(
      listOf(
        ListDetailNavStageStrategy(
          isListPane = { it is ListRole },
          isDetailPane = { it is DetailRole },
          isMultiPane = { true },
        )
      )
    )

  private fun advanceFramesComposingOnce(frames: Int) {
    repeat(frames) {
      composeTestRule.mainClock.advanceTimeByFrame()
      for (label in listOf("a", "s1", "s2")) {
        val count =
          composeTestRule
            .onAllNodesWithTag(label, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
        assertTrue(count <= 1, "'$label' composed $count times on frame $it")
      }
    }
  }

  private fun assertComposed(vararg labels: String) {
    for (label in labels) {
      composeTestRule.onAllNodesWithTag(label, useUnmergedTree = true).assertCountEquals(1)
    }
  }

  private fun setNavContent(screens: List<Screen>, decoration: NavDecoration): Navigator {
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val navStack = rememberSaveableNavStack(screens)
        navigator = rememberCircuitNavigator(navStack = navStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, navStack = navStack, decoration = decoration)
      }
    }
    return navigator
  }
}

private class SupportingPaneStage<T : NavArgument> : NavStage<T> {
  override val key: Any = "com.example.supporting-pane"

  override fun visibleItems(args: NavStackList<T>): List<T> {
    val main = args.backwardItems.firstOrNull() ?: return listOf(args.active)
    return listOf(main, args.active)
  }

  @Composable
  override fun Content(items: List<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    if (items.size == 1) {
      paneScope.Pane(key = "supporting", item = items.single(), modifier = modifier.fillMaxSize())
      return
    }
    val (main, supporting) = items
    Row(modifier.fillMaxSize()) {
      paneScope.Pane(key = "main", item = main, modifier = Modifier.weight(1f))
      paneScope.Pane(
        key = "supporting",
        item = supporting,
        modifier = Modifier.width(100.dp),
        transition = PaneTransition.Crossfade,
      )
    }
  }
}

private class IdHolder {
  var next = 0
}

private data class IdSlot<T : NavArgument>(val state: NavStageTransitionState<T>, val id: Int)

private object PreviewFlag {
  var on by mutableStateOf(false)
}

private object PreviewTransition : NavStageTransition {
  @Composable
  override fun <T : NavArgument> AnimatedStageContent(
    targetState: NavStageTransitionState<T>,
    stateFor: @Composable (NavStackList<T>) -> NavStageTransitionState<T>,
    navigator: Navigator,
    content: @Composable (NavStageTransitionState<T>) -> Unit,
  ) {
    val ids = remember { IdHolder() }
    val committed = remember(targetState) { IdSlot(targetState, ids.next++) }
    val popped =
      targetState.args.backwardItems
        .toList()
        .takeIf { it.isNotEmpty() }
        ?.let {
          navStackListOf(activeItem = it.first(), backwardItems = it.drop(1))
        }
    val previewState = if (PreviewFlag.on && popped != null) stateFor(popped) else null
    val previewSlot = remember(previewState) { previewState?.let { IdSlot(it, ids.next++) } }
    AnimatedContent(
      targetState = previewSlot ?: committed,
      contentKey = { it.id },
      transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
    ) { s ->
      content(s.state)
    }
  }
}

private data class RState(val label: String) : CircuitUiState

private interface ListRole

private interface DetailRole

private sealed class RScreen(val label: String) : Screen

private data object RA : RScreen("a"), ListRole

private data object RS1 : RScreen("s1"), ListRole, DetailRole

private data object RS2 : RScreen("s2"), DetailRole

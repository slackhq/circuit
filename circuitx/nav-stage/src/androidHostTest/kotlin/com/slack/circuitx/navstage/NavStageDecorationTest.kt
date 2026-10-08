// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalNavStageApi::class, ExperimentalSharedTransitionApi::class)

package com.slack.circuitx.navstage

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
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
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 34, maxSdk = 36)
@RunWith(RobolectricTestRunner::class)
class NavStageDecorationTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val mounts = mutableMapOf<String, Int>()

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, _, _ ->
        presenterOf { LabelState((screen as LabelScreen).label) }
      }
      .addUiFactory { _, _ ->
        ui<LabelState> { state, modifier ->
          remember { mounts.merge(state.label, 1, Int::plus) }
          BasicText(state.label, modifier.fillMaxSize().testTag(state.label))
        }
      }
      .build()

  @Test
  fun unfoldingMidCrossfadeComposesEachRecordOnce() {
    var multiPane by mutableStateOf(false)
    setNavContent(
      listOf(ItemList, Detail1),
      NavStageDecoration(
        strategies = listOf(ListDetailNavStageStrategy(isMultiPane = { multiPane })),
        stageTransition = NavStageTransition.Crossfade,
      ),
    )
    composeTestRule.mainClock.autoAdvance = false

    multiPane = true
    composeTestRule.waitForIdle()
    repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }
    assertComposedOnce(ItemList, Detail1)

    composeTestRule.mainClock.advanceTimeBy(1_000)
    assertComposedOnce(ItemList, Detail1)
  }

  @Test
  fun listDetailSplitsAndSwapsDetail() {
    val navigator =
      setNavContent(
        listOf(ItemList, Detail1),
        NavStageDecoration(listOf(ListDetailNavStageStrategy(isMultiPane = { true }))),
      )
    assertComposedOnce(ItemList, Detail1)

    navigator.goTo(Detail2)
    composeTestRule.waitForIdle()
    assertComposedOnce(ItemList, Detail2)
    assertNotComposed(Detail1)
  }

  @Test
  fun backGestureDuringPaneSlideComposesEachRecordOnce() {
    val navigator =
      setNavContent(listOf(Root), NavStageDecoration(emptyList(), GestureNavStageTransition()))
    composeTestRule.mainClock.autoAdvance = false

    navigator.goTo(Detail1)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(100)
    startBackGesture(progress = 0.5f)
    repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }
    assertComposedOnce(Root, Detail1)

    composeTestRule.mainClock.advanceTimeBy(1_000)
    assertComposedOnce(Root, Detail1)
  }

  @Test
  fun backGestureCommittedDuringPaneSlideComposesEachRecordOnce() {
    val navigator =
      setNavContent(listOf(Root), NavStageDecoration(emptyList(), GestureNavStageTransition()))
    composeTestRule.mainClock.autoAdvance = false

    navigator.goTo(Detail1)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(100)
    startBackGesture(progress = 0.5f)
    repeat(3) { composeTestRule.mainClock.advanceTimeByFrame() }
    composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    repeat(30) {
      composeTestRule.mainClock.advanceTimeByFrame()
      assertComposedOnce(Root)
    }
  }

  @Test
  fun swappingDetailKeepsListMounted() {
    val navigator =
      setNavContent(
        listOf(ItemList, Detail1),
        NavStageDecoration(
          listOf(ListDetailNavStageStrategy(isMultiPane = { true })),
          GestureNavStageTransition(),
        ),
        sharedElements = true,
      )
    composeTestRule.mainClock.autoAdvance = false

    navigator.goTo(Detail2)
    repeat(30) {
      composeTestRule.mainClock.advanceTimeByFrame()
      assertComposedOnce(ItemList)
    }
    assertEquals(1, mounts["list"])
  }

  @Test
  fun newStageInstanceWithSameKeyIsAdopted() {
    var tag by mutableStateOf("first")
    val strategy =
      object : NavStageStrategy {
        @Composable
        override fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T> =
          remember(tag) { TaggedStage(tag) }
      }
    setNavContent(listOf(Root), NavStageDecoration(listOf(strategy), GestureNavStageTransition()))
    composeTestRule.onNodeWithTag("first").assertExists()

    tag = "second"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("second").assertExists()
  }

  @Test
  fun completedBackGesturePops() {
    setNavContent(
      listOf(Root, Detail1),
      NavStageDecoration(emptyList(), GestureNavStageTransition()),
    )

    startBackGesture(progress = 0.5f)
    composeTestRule.waitForIdle()
    assertComposedOnce(Root, Detail1)

    composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    composeTestRule.waitForIdle()
    assertComposedOnce(Root)
    assertNotComposed(Detail1)
  }

  @Test
  fun cancelledBackGestureRestoresCurrentStage() {
    setNavContent(
      listOf(Root, Detail1),
      NavStageDecoration(emptyList(), GestureNavStageTransition()),
    )

    startBackGesture(progress = 0.5f)
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity {
      it.onBackPressedDispatcher.dispatchOnBackCancelled()
    }
    composeTestRule.waitForIdle()
    assertComposedOnce(Detail1)
    assertNotComposed(Root)
  }

  @Test
  fun backGestureWithinListDetailKeepsListSize() {
    setNavContent(
      listOf(ItemList, Detail1, Detail2),
      NavStageDecoration(
        listOf(ListDetailNavStageStrategy(isMultiPane = { true })),
        GestureNavStageTransition(),
      ),
      sharedElements = true,
    )
    val listHeight = composeTestRule.onNodeWithTag("list").getBoundsInRoot().height
    composeTestRule.mainClock.autoAdvance = false

    startBackGesture(progress = 0.5f)
    repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }
    assertEquals(listHeight, composeTestRule.onNodeWithTag("list").getBoundsInRoot().height)
  }

  private fun setNavContent(
    screens: List<Screen>,
    decoration: NavDecoration,
    sharedElements: Boolean = false,
  ): Navigator {
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit, CircuitSaver.NoOp) {
        val navStack = rememberSaveableNavStack(screens)
        navigator = rememberCircuitNavigator(navStack = navStack, onRootPop = {})
        if (sharedElements) {
          SharedElementTransitionLayout {
            NavigableCircuitContent(
              navigator = navigator,
              navStack = navStack,
              decoration = decoration,
            )
          }
        } else {
          NavigableCircuitContent(
            navigator = navigator,
            navStack = navStack,
            decoration = decoration,
          )
        }
      }
    }
    return navigator
  }

  private fun startBackGesture(progress: Float) {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val event =
        BackEventCompat(
          touchX = 0f,
          touchY = activity.window.decorView.height / 2f,
          progress = 0f,
          swipeEdge = BackEventCompat.EDGE_LEFT,
        )
      with(activity.onBackPressedDispatcher) {
        dispatchOnBackStarted(event)
        dispatchOnBackProgressed(event)
        dispatchOnBackProgressed(
          BackEventCompat(touchX = 40f, touchY = event.touchY, progress, event.swipeEdge)
        )
      }
    }
  }

  private fun assertComposedOnce(vararg screens: LabelScreen) {
    screens.forEach { composeTestRule.onAllNodesWithTag(it.label).assertCountEquals(1) }
  }

  private fun assertNotComposed(screen: LabelScreen) {
    composeTestRule.onAllNodesWithTag(screen.label).assertCountEquals(0)
  }
}

private class TaggedStage<T : NavArgument>(private val tag: String) : NavStage<T> {
  override val key: Any = "tagged"

  override fun visibleItems(args: NavStackList<T>): List<T> = listOf(args.active)

  @Composable
  override fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    Box(modifier.testTag(tag)) { paneScope.Pane(key = "main", item = args.active) }
  }
}

private data class LabelState(val label: String) : CircuitUiState

private sealed class LabelScreen(val label: String) : Screen

private data object Root : LabelScreen("root")

private data object ItemList : LabelScreen("list"), ListPane

private data object Detail1 : LabelScreen("detail1"), DetailPane

private data object Detail2 : LabelScreen("detail2"), DetailPane

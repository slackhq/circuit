// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import kotlin.test.Test

/** Tests [rememberPaneNavigation] with its default arguments. */
@OptIn(ExperimentalTestApi::class)
class PaneNavigationTest {
  private data object Root : Screen

  private data class Detail(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.LIST_DETAIL
  }

  private data class TestState(val screen: Screen) : CircuitUiState

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, _, _ -> presenterOf { TestState(screen) } }
      .addUiFactory { _, _ -> ui<TestState> { state, _ -> BasicText("${state.screen}") } }
      .build()

  @Test
  fun defaultsShowPanesAndReplaceTheDetail() = runComposeUiTest {
    lateinit var panes: PaneNavigation
    lateinit var navigator: Navigator
    setContent {
      CircuitCompositionLocals(circuit) {
        // Wide enough for two panes
        Box(Modifier.size(1000.dp, 700.dp)) {
          val backStack = rememberSaveableBackStack(Root)
          navigator = rememberCircuitNavigator(backStack) {}
          panes = rememberPaneNavigation(navigator)
          NavigableCircuitContent(panes.navigator, backStack, decoration = panes.decoration)
        }
      }
    }

    runOnIdle { panes.navigator.goTo(Detail(1)) }
    onNodeWithText("$Root").assertExists()
    onNodeWithText("${Detail(1)}").assertExists()
    runOnIdle { check(panes.host.isSplit) }

    // The navigator has the replace interceptor
    runOnIdle { panes.navigator.goTo(Detail(2)) }
    onNodeWithText("${Detail(2)}").assertExists()
    onAllNodesWithText("${Detail(1)}").assertCountEquals(0)
    runOnIdle { check(navigator.peekBackStack() == listOf(Detail(2), Root)) }
  }

  @Test
  fun replacedDetailCrossfades() = runComposeUiTest {
    lateinit var panes: PaneNavigation
    setContent {
      CircuitCompositionLocals(circuit) {
        Box(Modifier.size(1000.dp, 700.dp)) {
          val backStack = rememberSaveableBackStack(Root)
          val navigator = rememberCircuitNavigator(backStack) {}
          panes = rememberPaneNavigation(navigator)
          NavigableCircuitContent(panes.navigator, backStack, decoration = panes.decoration)
        }
      }
    }
    runOnIdle { panes.navigator.goTo(Detail(1)) }
    val settled = onNodeWithText("${Detail(1)}").getUnclippedBoundsInRoot()

    // Stop part of the way through the replace, while both screens draw
    mainClock.autoAdvance = false
    runOnUiThread { panes.navigator.goTo(Detail(2)) }
    mainClock.advanceTimeBy(50)
    onNodeWithText("${Detail(1)}").assertExists()
    onNodeWithText("${Detail(2)}").assertExists()

    mainClock.autoAdvance = true
    waitForIdle()
    onAllNodesWithText("${Detail(1)}").assertCountEquals(0)
    val replaced = onNodeWithText("${Detail(2)}").getUnclippedBoundsInRoot()
    check(replaced == settled) { "replaced: $replaced, settled: $settled" }
  }

  private fun arg(id: Int, role: PaneRole): PaneArg<Arg> {
    return PaneArg.Record(Arg(Detail(id)), role)
  }

  /** The change from a pane stack with [from] on top to one with [to] on top. */
  private fun change(from: PaneArg<Arg>, to: PaneArg<Arg>): PaneChange? {
    return paneChange(navStackListOf(activeItem = from), navStackListOf(activeItem = to))
  }

  @Test
  fun firstScreenOfAHiddenPaneOpensThePane() {
    val hidden = PaneArg.Placeholder(shown = false)
    check(change(hidden, arg(1, PaneRole.DETAIL_ROOT)) == PaneChange.OPEN)
  }

  @Test
  fun newBottomScreenOfAVisiblePaneReplaces() {
    val shown = PaneArg.Placeholder(shown = true)
    check(change(shown, arg(1, PaneRole.DETAIL_ROOT)) == PaneChange.REPLACE)
    check(change(arg(1, PaneRole.DETAIL_ROOT), arg(2, PaneRole.DETAIL_ROOT)) == PaneChange.REPLACE)
    // A screen stacked over the old detail goes with it
    check(
      change(arg(1, PaneRole.DETAIL_STACKED), arg(2, PaneRole.DETAIL_ROOT)) == PaneChange.REPLACE
    )
    check(change(arg(1, PaneRole.EXTRA_ROOT), arg(2, PaneRole.EXTRA_ROOT)) == PaneChange.REPLACE)
  }

  @Test
  fun otherChangesKeepTheTransitionOfTheDelegate() {
    // A push over a detail
    check(change(arg(1, PaneRole.DETAIL_ROOT), arg(2, PaneRole.DETAIL_STACKED)) == null)
    // A switch from the single layout
    check(change(arg(1, PaneRole.SINGLE), arg(2, PaneRole.DETAIL_ROOT)) == null)
    check(change(arg(1, PaneRole.SINGLE), arg(2, PaneRole.SINGLE)) == null)
    // The same screen
    check(change(arg(1, PaneRole.DETAIL_ROOT), arg(1, PaneRole.DETAIL_ROOT)) == null)
  }

  private data class Arg(override val screen: Screen) : NavArgument {
    override val key: String
      get() = screen.toString()
  }
}

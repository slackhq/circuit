// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.test.Test

/**
 * Tests shared elements between two screens of the end pane, like an avatar that moves from a
 * message to the profile pushed over it.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class PaneSharedElementTest {
  private data object Inbox : Screen

  /** A detail with a small avatar. */
  private data object Message : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.ListDetail
  }

  /**
   * A screen pushed over the detail, with a large avatar in another place. It has no pane style, so
   * it stays in the pane of the detail that opened it.
   */
  private data object Profile : Screen

  private data class TestState(val screen: Screen) : CircuitUiState

  /** The shared state of the avatar on each screen that has composed. */
  private val avatarStates = mutableMapOf<Screen, SharedContentState>()

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, _, _ -> presenterOf { TestState(screen) } }
      .addUiFactory { _, _ ->
        ui<TestState> { state, modifier ->
          SharedElementTransitionScope {
            val avatar = rememberSharedContentState(key = "avatar")
            avatarStates[state.screen] = avatar
            val shared = Modifier.sharedElement(avatar, requireAnimatedScope(Navigation))
            Box(modifier.fillMaxSize().testTag("${state.screen}")) {
              when (state.screen) {
                Message -> Box(shared.size(40.dp))
                Profile ->
                  Box(Modifier.padding(start = 100.dp, top = 200.dp).then(shared).size(120.dp))
                else -> Unit
              }
            }
          }
        }
      }
      .build()

  private lateinit var panes: PaneNavigation

  private fun ComposeUiTest.setNavContent(size: DpSize) {
    setContent {
      CircuitCompositionLocals(circuit) {
        SharedElementTransitionLayout {
          Box(Modifier.size(size)) {
            val backStack = rememberSaveableBackStack(Inbox)
            val navigator = rememberCircuitNavigator(backStack) {}
            panes = rememberPaneNavigation(navigator)
            NavigableCircuitContent(panes.navigator, backStack, decoration = panes.decoration)
          }
        }
      }
    }
  }

  /** Opens the profile over the message and reports whether the avatars matched part way in. */
  private fun ComposeUiTest.avatarsMatchDuringThePush(): Boolean {
    runOnIdle { panes.navigator.goTo(Message) }
    waitForIdle()
    mainClock.autoAdvance = false
    runOnUiThread { panes.navigator.goTo(Profile) }
    mainClock.advanceTimeBy(100)
    val matched = runOnUiThread {
      avatarStates.getValue(Message).isMatchFound && avatarStates.getValue(Profile).isMatchFound
    }
    mainClock.autoAdvance = true
    waitForIdle()
    // No match is left once the message is hidden below the profile
    check(!runOnUiThread { avatarStates.getValue(Profile).isMatchFound })
    return matched
  }

  @Test
  fun pushOverADetailSharesElementsInTheEndPane() = runComposeUiTest {
    setNavContent(DpSize(1000.dp, 700.dp))
    val matched = avatarsMatchDuringThePush()
    // The list stays in the start pane while the profile covers the message beside it
    onNodeWithTag("$Inbox").assertExists()
    check(panes.host.isSplit)
    check(matched) { "The avatars did not match in the end pane" }
  }

  @Test
  fun pushOverADetailSharesElementsInTheSingleLayout() = runComposeUiTest {
    setNavContent(DpSize(400.dp, 700.dp))
    val matched = avatarsMatchDuringThePush()
    check(!panes.host.isSplit)
    check(matched) { "The avatars did not match in the single layout" }
  }
}

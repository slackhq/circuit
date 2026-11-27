// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.ComposeUiTestRunner
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.RunWith
import com.slack.circuit.foundation.TestScreen
import com.slack.circuit.foundation.TestScreen2
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(
  ExperimentalTestApi::class,
  ExperimentalCircuitApi::class,
  ExperimentalSharedTransitionApi::class,
)
@RunWith(ComposeUiTestRunner::class)
class OnNavEventSharedElementNavDecorationTest {

  private val hasNavigationScope = mutableMapOf<Screen, Boolean>()
  private lateinit var navigator: Navigator

  private val circuit =
    Circuit.Builder()
      .addPresenter<TestScreen, ReportState> { screen, _, _ -> presenterOf { ReportState(screen) } }
      .addPresenter<TestScreen2, ReportState> { screen, _, _ ->
        presenterOf { ReportState(screen) }
      }
      .addUi<TestScreen, ReportState> { state, _ -> ReportingUi(state.screen) }
      .addUi<TestScreen2, ReportState> { state, _ -> ReportingUi(state.screen) }
      .build()

  @Test
  fun `navigation scope is only provided during a transition`() = runComposeUiTest {
    setNavContent(withLayout = false)
    waitForIdle()
    assertFalse(hasNavigationScope.getValue(TestScreen))

    mainClock.autoAdvance = false
    navigator.goTo(TestScreen2)
    waitForIdle()
    mainClock.advanceTimeBy(100)
    waitForIdle()
    assertTrue(hasNavigationScope.getValue(TestScreen))
    assertTrue(hasNavigationScope.getValue(TestScreen2))

    mainClock.autoAdvance = true
    waitForIdle()
    assertFalse(hasNavigationScope.getValue(TestScreen2))
  }

  @Test
  fun `uses an existing shared element layout`() = runComposeUiTest {
    setNavContent(withLayout = true)
    waitForIdle()
    assertTrue(hasNavigationScope.getValue(TestScreen))

    navigator.goTo(TestScreen2)
    waitForIdle()
    assertTrue(hasNavigationScope.getValue(TestScreen2))
  }

  private fun ComposeUiTest.setNavContent(withLayout: Boolean) {
    val decoration =
      OnNavEventSharedElementNavDecoration(
        animatedScreenTransforms = emptyMap(),
        decoratorFactory = NavigatorDefaults.DefaultDecoratorFactory,
      )
    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen)
        navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
        val content =
          @Composable {
            NavigableCircuitContent(
              navigator = navigator,
              backStack = backStack,
              decoration = decoration,
            )
          }
        if (withLayout) SharedElementTransitionLayout { content() } else content()
      }
    }
  }

  @Composable
  private fun ReportingUi(screen: Screen) {
    if (SharedElementTransitionScope.isAvailable) {
      SharedElementTransitionScope {
        val navigation = findAnimatedScope(Navigation) != null
        SideEffect { hasNavigationScope[screen] = navigation }
      }
    } else {
      SideEffect { hasNavigationScope[screen] = false }
    }
  }
}

private data class ReportState(val screen: Screen) : CircuitUiState

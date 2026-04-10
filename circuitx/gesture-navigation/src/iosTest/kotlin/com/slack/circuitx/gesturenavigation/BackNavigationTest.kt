// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContent
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestEvent
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.TestState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.resetRoot
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import kotlin.test.Test

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
class BackNavigationTest {

  @Test
  fun testBackHandlerRoot() {
    runComposeUiTest {
      setCircuitContent(useIntegratedBackHandler = false)

      // Current: Root Alpha. Navigate to Screen A
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")
      println("Going to A")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
      println("Screen A")

      println("Going to B")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
      println("Screen B")
    }
  }

  @Test
  fun testIntegratedBackHandlerRoot() {
    runComposeUiTest {
      setCircuitContent(useIntegratedBackHandler = true)

      // Current: Root Alpha. Navigate to Screen A
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")
      println("Going to A")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
      println("Screen A")

      println("Going to B")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
      println("Screen B")
    }
  }

  private fun ComposeUiTest.setCircuitContent(useIntegratedBackHandler: Boolean) {
    val circuit = createTestBackCircuit()
    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen.RootAlpha)
        val navigator: Navigator
        if (useIntegratedBackHandler) {
          navigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = {},
              enableBackHandler = true,
            )
        } else {
          navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
          NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) {
            navigator.pop()
          }
        }
        NavigableCircuitContent(
          navigator = navigator,
          backStack = backStack,
          decoratorFactory =
            remember { IOSPredictiveBackNavDecorator.Factory(onBackInvoked = navigator::pop) },
        )
      }
    }
  }
}

fun createTestBackCircuit(
  presenter: (Screen, Navigator) -> Presenter<*> = { screen, navigator ->
    TestCountBackPresenter(screen = screen as TestScreen, navigator = navigator)
  }
): Circuit =
  Circuit.Builder()
    .addPresenterFactory { screen, navigator, _ -> presenter(screen, navigator) }
    .addUiFactory { _, _ -> ui<TestState> { state, modifier -> TestBackContent(state, modifier) } }
    .build()

@Composable
fun TestBackContent(state: TestState, modifier: Modifier = Modifier) {
  TestContent(state, modifier)
  if (state.label.contains("root", true)) {
    NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) {}
  }
}

class TestCountBackPresenter(
  private val screen: TestScreen,
  private val navigator: Navigator,
  private val saveStateOnRootChange: Boolean = false,
  private val restoreStateOnRootChange: Boolean = false,
) : Presenter<TestState> {
  @Composable
  override fun present(): TestState {
    var count by rememberSaveable { mutableIntStateOf(0) }
    return TestState(count, screen.label) { event ->
      when (event) {
        TestEvent.IncreaseCount -> count++
        TestEvent.PopNavigation -> navigator.pop()
        TestEvent.GoToNextScreen -> {
          when (screen) {
            // Root screens all go to ScreenA
            TestScreen.RootAlpha -> navigator.goTo(TestScreen.ScreenA)
            TestScreen.RootBeta -> navigator.goTo(TestScreen.ScreenA)
            // Otherwise each screen navigates to the next screen
            TestScreen.ScreenA -> {
              navigator.resetRoot(TestScreen.RootAlpha)
              navigator.goTo(TestScreen.ScreenB)
            }
            TestScreen.ScreenB -> {
              navigator.resetRoot(TestScreen.RootAlpha)
              navigator.goTo(TestScreen.ScreenC)
            }
            else -> error("Can't navigate from $screen")
          }
        }
        TestEvent.ResetRootAlpha ->
          navigator.resetRoot(TestScreen.RootAlpha, true, restoreStateOnRootChange)
        TestEvent.ResetRootBeta ->
          navigator.resetRoot(TestScreen.RootBeta, saveStateOnRootChange, restoreStateOnRootChange)
      }
    }
  }
}

// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.ui
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterialApi::class)
@Config(minSdk = 34)
@RunWith(RobolectricTestRunner::class)
class GestureNavigationCrashTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `gesture crash`() = runTest {
    composeTestRule.run {
      val circuit =
        Circuit.Builder()
          .addPresenterFactory { screen, navigator, _ ->
            TestCrashPresenter(screen as TestScreen, navigator)
          }
          .addUiFactory { _, _ ->
            ui<TestCrashState> { state, modifier -> TestCrashContent(state, modifier) }
          }
          .build()

      lateinit var navigator: Navigator
      setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack(TestScreen.RootAlpha)
          navigator = rememberCircuitNavigator(backStack = backStack)
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            decoratorFactory =
              remember { AndroidPredictiveBackNavDecorator.Factory(onBackInvoked = navigator::pop) },
          )
        }
      }

      // current alpha at startup
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")

      // reset to beta with saveState=true restoreState=true
      navigator.resetRoot(TestScreen.RootBeta, saveState = true, restoreState = true)
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Beta")

      // reset back to alpha with saveState=true restoreState=true
      navigator.resetRoot(TestScreen.RootAlpha, saveState = true, restoreState = true)
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")

      // go to A
      navigator.goTo(TestScreen.ScreenA)
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")

      // reset to beta with saveState=true restoreState=false - !!! important to be false not true
      // !!!
      navigator.resetRoot(TestScreen.RootBeta, saveState = true, restoreState = false)
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Beta")

      // reset back to alpha with saveState=true restoreState=true
      navigator.resetRoot(TestScreen.RootAlpha, saveState = true, restoreState = true)
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")

      // pop Screen A - should go to alpha root
      activityRule.scenario.performGestureNavigationBackSwipe()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")

      // reset back to beta with saveState=true restoreState=true - will crash if previous pop was
      // done with gesture navigation
      navigator.resetRoot(TestScreen.RootBeta, saveState = true, restoreState = true)
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Beta")
    }
  }
}

private class TestCrashPresenter(private val screen: TestScreen, private val navigator: Navigator) :
  Presenter<TestCrashState> {
  @Composable
  override fun present(): TestCrashState {
    return TestCrashState(screen.label) { event ->
      when (event) {
        TestCrashEvent.PopNavigation -> navigator.pop()
        TestCrashEvent.GoToNextScreen -> {
          when (screen) {
            // Root screens all go to ScreenA
            TestScreen.RootAlpha -> navigator.goTo(TestScreen.ScreenA)
            TestScreen.RootBeta -> navigator.goTo(TestScreen.ScreenA)
            // Otherwise each screen navigates to the next screen
            TestScreen.ScreenA -> navigator.goTo(TestScreen.ScreenB)
            TestScreen.ScreenB -> navigator.goTo(TestScreen.ScreenC)
            else -> error("Can't navigate from $screen")
          }
        }
        is TestCrashEvent.ResetRootAlpha ->
          navigator.resetRoot(TestScreen.RootAlpha, true, event.restoreState)
        is TestCrashEvent.ResetRootBeta ->
          navigator.resetRoot(TestScreen.RootBeta, true, event.restoreState)
      }
    }
  }
}

@Composable
private fun TestCrashContent(state: TestCrashState, modifier: Modifier = Modifier) {
  Box(modifier = modifier.testTag(TestContentTags.TAG_ROOT)) {
    BasicText(text = state.label, modifier = Modifier.testTag(TAG_LABEL))
  }
}

data class TestCrashState(val label: String, val eventSink: (TestCrashEvent) -> Unit) :
  CircuitUiState

sealed interface TestCrashEvent {
  data object GoToNextScreen : TestCrashEvent

  data object PopNavigation : TestCrashEvent

  data class ResetRootAlpha(val restoreState: Boolean = true) : TestCrashEvent

  data class ResetRootBeta(val restoreState: Boolean = true) : TestCrashEvent
}

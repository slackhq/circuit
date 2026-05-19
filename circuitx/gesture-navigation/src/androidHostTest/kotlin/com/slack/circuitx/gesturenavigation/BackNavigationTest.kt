// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
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
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 34)
@RunWith(ParameterizedRobolectricTestRunner::class)
class BackNavigationTest(private val androidNavigator: Boolean) {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun testBackHandlerRoot() {
    composeTestRule.run {
      val circuit = createTestBackCircuit()
      setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack(TestScreen.RootAlpha)
          val navigator: Navigator
          if (androidNavigator) {
            navigator = rememberCircuitNavigator(backStack = backStack, enableBackHandler = true)
          } else {
            navigator =
              rememberCircuitNavigator(
                backStack = backStack,
                onRootPop = {}, // no-op for tests
              )
            NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) {
              navigator.pop()
            }
          }
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            decoratorFactory =
              remember { AndroidPredictiveBackNavDecorator.Factory(onBackInvoked = navigator::pop) },
          )
        }
      }

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

      // tap back
      println("Going to Root Alpha via back button")
      this.activity.onBackPressedDispatcher.onBackPressed()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("Root Alpha")
    }
  }

  companion object {

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "androidNavigator={0}")
    fun params(): List<Any> {
      return listOf(true, false)
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
    BackHandler {
      // no-op for now
    }
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
          navigator.resetRoot(
            TestScreen.RootAlpha,
            StateOptions(save = true, restore = restoreStateOnRootChange, clear = false),
          )
        TestEvent.ResetRootBeta ->
          navigator.resetRoot(
            TestScreen.RootBeta,
            StateOptions(
              save = saveStateOnRootChange,
              restore = restoreStateOnRootChange,
              clear = false,
            ),
          )
      }
    }
  }
}

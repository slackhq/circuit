// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.TestContentTags.TAG_COUNT
import com.slack.circuit.foundation.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.foundation.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.foundation.TestContentTags.TAG_LABEL
import com.slack.circuit.foundation.TestContentTags.TAG_POP
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitRetainedStateTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test fun retainedStateScopedToBackstackWithKeys() = retainedStateScopedToBackstack(true)

  @Test fun retainedStateScopedToBackstackWithoutKeys() = retainedStateScopedToBackstack(false)

  private fun retainedStateScopedToBackstack(useKeys: Boolean) {
    composeTestRule.run {
      val circuit =
        Circuit.Builder()
          .addPresenterFactory { screen, navigator, _ ->
            TestCountPresenter(screen as TestScreen, navigator, useKeys)
          }
          .addUiFactory { _, _ ->
            ui<TestState> { state, modifier -> TestContent(state, modifier) }
          }
          .build()

      setContent {
        CircuitCompositionLocals(circuit) {
          val backstack = rememberSaveableBackStack { push(TestScreen.ScreenA) }
          val navigator =
            rememberCircuitNavigator(
              backstack = backstack,
              onRootPop = {} // no-op for tests
            )
          NavigableCircuitContent(navigator = navigator, backstack = backstack)
        }
      }

      // Current: Screen A. Increase count to 1
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen C. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Pop to Screen B. Increase count from 1 to 2.
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Navigate to Screen C. Assert that it's state was not retained
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Pop to Screen B. Assert that it's state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Pop to Screen A. Assert that it's state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Assert that it's state was not retained
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }

  private sealed class TestScreen(val label: String) : Screen {

    @Parcelize data object ScreenA : TestScreen("A")

    @Parcelize data object ScreenB : TestScreen("B")

    @Parcelize data object ScreenC : TestScreen("C")
  }

  @Composable
  private fun TestContent(state: TestState, modifier: Modifier) {
    Column(modifier = modifier) {
      BasicText(text = state.label, modifier = Modifier.testTag(TAG_LABEL))

      BasicText(text = "${state.count}", modifier = Modifier.testTag(TAG_COUNT))

      BasicText(
        text = "Increase count",
        modifier =
          Modifier.testTag(TAG_INCREASE_COUNT).clickable {
            state.eventSink(TestEvent.IncreaseCount)
          }
      )

      BasicText(
        text = "Pop",
        modifier = Modifier.testTag(TAG_POP).clickable { state.eventSink(TestEvent.PopNavigation) }
      )
      BasicText(
        text = "Go to next",
        modifier =
          Modifier.testTag(TAG_GO_NEXT).clickable { state.eventSink(TestEvent.GoToNextScreen) }
      )
    }
  }

  private class TestCountPresenter(
    private val screen: TestScreen,
    private val navigator: Navigator,
    private val useKeys: Boolean,
  ) : Presenter<TestState> {
    @Composable
    override fun present(): TestState {
      var launchCount by rememberRetained(key = "count".takeIf { useKeys }) { mutableIntStateOf(0) }

      return TestState(launchCount, screen.label) { event ->
        when (event) {
          TestEvent.IncreaseCount -> launchCount++
          TestEvent.PopNavigation -> navigator.pop()
          TestEvent.GoToNextScreen -> {
            when (screen) {
              is TestScreen.ScreenA -> navigator.goTo(TestScreen.ScreenB)
              is TestScreen.ScreenB -> navigator.goTo(TestScreen.ScreenC)
              else -> error("Can't navigate from $screen")
            }
          }
        }
      }
    }
  }

  private data class TestState(
    val count: Int,
    val label: String,
    val eventSink: (TestEvent) -> Unit
  ) : CircuitUiState

  private sealed interface TestEvent {
    data object GoToNextScreen : TestEvent

    data object PopNavigation : TestEvent

    data object IncreaseCount : TestEvent
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStackRecordLocalProvider
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class ProvidedValuesLifetimeTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun providedValuesAvailableDuringPop() {
    composeTestRule.run {
      mainClock.autoAdvance = false

      val circuit =
        Circuit.Builder()
          .addPresenterFactory { screen, navigator, _ ->
            TestPresenter(
              screen as TestScreen,
              navigator,
            )
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
          NavigableCircuitContent(
            navigator = navigator,
            backstack = backstack,
            providedValues =
              providedValuesForBackStack(
                backStack = backstack,
                stackLocalProviders = persistentListOf(TestBackStackRecordLocalProvider),
              )
          )
        }
      }

      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("A")

      // Navigate to Screen B
      onNodeWithTag(TestContentTags.TAG_GO_NEXT).performClick()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("B")

      // Navigate to Screen C
      onNodeWithTag(TestContentTags.TAG_GO_NEXT).performClick()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("C")

      // Pop to Screen B
      onNodeWithTag(TestContentTags.TAG_POP).performClick()

      mainClock.advanceTimeBy(1_000)
      waitForIdle()

      // Half-way through pop, both screens should be visible
      onAllNodesWithTag(TestContentTags.TAG_LABEL)
        .assertCountEquals(2)
        .assertAny(hasTextExactly("C"))
        .assertAny(hasTextExactly("B"))

      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("B")

      // Pop to Screen A
      onNodeWithTag(TestContentTags.TAG_POP).performClick()

      mainClock.advanceTimeBy(1_000)
      waitForIdle()

      // Half-way through pop, both screens should be visible
      onAllNodesWithTag(TestContentTags.TAG_LABEL)
        .assertCountEquals(2)
        .assertAny(hasTextExactly("B"))
        .assertAny(hasTextExactly("A"))

      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("A")
    }
  }

  companion object {
    val LocalWithDefault = compositionLocalOf<String> { "Default" }
  }

  private object TestBackStackRecordLocalProvider : BackStackRecordLocalProvider<BackStack.Record> {
    @Composable
    override fun providedValuesFor(record: BackStack.Record): ProvidedValues {
      return ProvidedValues {
        persistentListOf(LocalWithDefault provides (record.screen as TestScreen).label)
      }
    }
  }

  @Composable
  private fun TestContent(state: TestState, modifier: Modifier) {
    Column(modifier = modifier) {
      BasicText(text = state.label, modifier = Modifier.testTag(TestContentTags.TAG_LABEL))

      BasicText(
        text = "Pop",
        modifier =
          Modifier.testTag(TestContentTags.TAG_POP).clickable {
            state.eventSink(TestEvent.PopNavigation)
          }
      )
      BasicText(
        text = "Go to next",
        modifier =
          Modifier.testTag(TestContentTags.TAG_GO_NEXT).clickable {
            state.eventSink(TestEvent.GoToNextScreen)
          }
      )
    }
  }

  private class TestPresenter(
    private val screen: TestScreen,
    private val navigator: Navigator,
  ) : Presenter<TestState> {
    @Composable
    override fun present(): TestState {
      return TestState(label = LocalWithDefault.current) {
        when (it) {
          TestEvent.PopNavigation -> navigator.pop()
          TestEvent.GoToNextScreen ->
            when (screen) {
              is TestScreen.ScreenA -> navigator.goTo(TestScreen.ScreenB)
              is TestScreen.ScreenB -> navigator.goTo(TestScreen.ScreenC)
              else -> error("Can't navigate from $screen")
            }
        }
      }
    }
  }

  private sealed class TestScreen(val label: String) : Screen {

    @Parcelize data object ScreenA : TestScreen("A")

    @Parcelize data object ScreenB : TestScreen("B")

    @Parcelize data object ScreenC : TestScreen("C")
  }

  private data class TestState(val label: String, val eventSink: (TestEvent) -> Unit) :
    CircuitUiState

  private sealed interface TestEvent {
    data object GoToNextScreen : TestEvent

    data object PopNavigation : TestEvent
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.slack.circuit.internal.test.TestContentTags
import com.slack.circuit.internal.test.TestEvent
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.TestState
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
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
        createTestCircuit(
          presenter = { screen, navigator -> TestPresenter(screen as TestScreen, navigator) }
        )

      setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          val navigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = {}, // no-op for tests
            )
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            providedValues =
              providedValuesForBackStack(
                backStack = backStack,
                backStackLocalProviders = listOf(TestBackStackRecordLocalProvider),
              ),
          )
        }
      }

      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("Local A")

      // Navigate to Screen B
      onNodeWithTag(TestContentTags.TAG_GO_NEXT).performClick()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("Local B")

      // Navigate to Screen C
      onNodeWithTag(TestContentTags.TAG_GO_NEXT).performClick()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      mainClock.advanceTimeBy(1_000)
      waitForIdle()
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("Local C")

      // Pop to Screen B
      onNodeWithTag(TestContentTags.TAG_POP).performClick()

      // Part-way through pop, both screens should be visible
      repeat(10) {
        mainClock.advanceTimeByFrame()
        onAllNodesWithTag(TestContentTags.TAG_LABEL)
          .assertCountEquals(2)
          .assertAny(hasTextExactly("Local C"))
          .assertAny(hasTextExactly("Local B"))
      }

      mainClock.advanceTimeBy(1_000)
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("Local B")

      // Pop to Screen A
      onNodeWithTag(TestContentTags.TAG_POP).performClick()

      // Part-way through pop, both screens should be visible
      repeat(10) {
        mainClock.advanceTimeByFrame()
        onAllNodesWithTag(TestContentTags.TAG_LABEL)
          .assertCountEquals(2)
          .assertAny(hasTextExactly("Local B"))
          .assertAny(hasTextExactly("Local A"))
      }

      mainClock.advanceTimeBy(1_000)
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("Local A")
    }
  }

  companion object {
    val LocalWithDefault = compositionLocalOf { "Default" }
  }

  class TestPresenter(private val screen: TestScreen, private val navigator: Navigator) :
    Presenter<TestState> {
    @Composable
    override fun present(): TestState {
      var count by remember { mutableIntStateOf(0) }

      return TestState(count, LocalWithDefault.current) { event ->
        when (event) {
          TestEvent.IncreaseCount -> count++
          TestEvent.PopNavigation -> navigator.pop()
          TestEvent.GoToNextScreen -> {
            when (screen) {
              is TestScreen.ScreenA -> navigator.goTo(TestScreen.ScreenB)
              is TestScreen.ScreenB -> navigator.goTo(TestScreen.ScreenC)
              else -> error("Can't navigate from $screen")
            }
          }
          else -> {
            /* no-op */
          }
        }
      }
    }
  }

  private object TestBackStackRecordLocalProvider : BackStackRecordLocalProvider<BackStack.Record> {
    @Composable
    override fun providedValuesFor(record: BackStack.Record): ProvidedValues {
      return ProvidedValues {
        persistentListOf(
          LocalWithDefault provides remember { "Local ${(record.screen as TestScreen).label}" }
        )
      }
    }
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
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

      val circuit = createTestCircuit()

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

      // Part-way through pop, both screens should be visible
      mainClock.advanceTimeByFrame()
      onAllNodesWithTag(TestContentTags.TAG_LABEL)
        .assertCountEquals(2)
        .assertAny(hasTextExactly("C"))
        .assertAny(hasTextExactly("B"))

      mainClock.advanceTimeBy(1_000)
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("B")

      // Pop to Screen A
      onNodeWithTag(TestContentTags.TAG_POP).performClick()

      // Part-way through pop, both screens should be visible
      mainClock.advanceTimeByFrame()
      onAllNodesWithTag(TestContentTags.TAG_LABEL)
        .assertCountEquals(2)
        .assertAny(hasTextExactly("B"))
        .assertAny(hasTextExactly("A"))

      mainClock.advanceTimeBy(1_000)
      onNodeWithTag(TestContentTags.TAG_LABEL).assertTextEquals("A")
    }
  }

  companion object {
    val LocalWithDefault = compositionLocalOf { "Default" }
  }

  private object TestBackStackRecordLocalProvider : BackStackRecordLocalProvider<BackStack.Record> {
    @Composable
    override fun providedValuesFor(record: BackStack.Record): ProvidedValues = ProvidedValues {
      persistentListOf(LocalWithDefault provides (record.screen as TestScreen).label)
    }
  }
}

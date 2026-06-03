// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitViewModelStateAndroidTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<NavigableCircuitViewModelStateTestActivity>()

  private val scenario: ActivityScenario<NavigableCircuitViewModelStateTestActivity>
    get() = composeTestRule.activityRule.scenario

  @Test
  fun retainedStateScopedToBackstackWithRecreations() {
    composeTestRule.run {
      mainClock.autoAdvance = true

      // Current: Screen A. Increase count to 1
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen C. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      mainClock.withAutoAdvance(false) {
        // Pop to Screen B
        onNodeWithTag(TAG_POP).performClick()

        // Part-way through pop, both screens should be visible
        onEachFrameWhileMultipleScreens(hasTestTag(TAG_LABEL)) {
          onAllNodesWithTag(TAG_LABEL)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("C"))
            .assertAny(hasTextExactly("B"))
          onAllNodesWithTag(TAG_COUNT).assertCountEquals(2).assertAll(hasTextExactly("1"))
        }
      }

      // Increase count from 1 to 2.
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      mainClock.withAutoAdvance(false) {
        // Navigate to Screen C
        onNodeWithTag(TAG_GO_NEXT).performClick()

        // Part-way through push, both screens should be visible
        onEachFrameWhileMultipleScreens(hasTestTag(TAG_LABEL)) {
          onAllNodesWithTag(TAG_LABEL)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("C"))
            .assertAny(hasTextExactly("B"))
          onAllNodesWithTag(TAG_COUNT)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("0"))
            .assertAny(hasTextExactly("2"))
        }
      }
      // Assert that Screen C's state was retained
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      mainClock.withAutoAdvance(false) {
        // Pop to Screen B
        onNodeWithTag(TAG_POP).performClick()

        // Part-way through pop, both screens should be visible
        onEachFrameWhileMultipleScreens(hasTestTag(TAG_LABEL)) {
          onAllNodesWithTag(TAG_LABEL)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("C"))
            .assertAny(hasTextExactly("B"))
          onAllNodesWithTag(TAG_COUNT)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("0"))
            .assertAny(hasTextExactly("2"))
        }
      }
      // Assert that Screen B's state was retained
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      mainClock.withAutoAdvance(false) {
        // Pop to Screen A
        onNodeWithTag(TAG_POP).performClick()

        // Part-way through pop, both screens should be visible
        onEachFrameWhileMultipleScreens(hasTestTag(TAG_LABEL)) {
          onAllNodesWithTag(TAG_LABEL)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("B"))
            .assertAny(hasTextExactly("A"))
          onAllNodesWithTag(TAG_COUNT)
            .assertCountEquals(2)
            .assertAny(hasTextExactly("2"))
            .assertAny(hasTextExactly("1"))
        }
      }
      // Assert that Screen B's state was retained
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Assert that it's state was not retained
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }

  private fun ComposeTestRule.onEachFrameWhileMultipleScreens(
    matcher: SemanticsMatcher,
    block: ComposeTestRule.() -> Unit,
  ) {
    var i = 0
    while (true) {
      mainClock.advanceTimeByFrame()
      if (onAllNodes(matcher).fetchSemanticsNodes().size <= 1) {
        break
      }
      try {
        block()
      } catch (e: Throwable) {
        throw AssertionError("Error on frame $i", e)
      }
      i++
    }
  }
}

private fun MainTestClock.withAutoAdvance(value: Boolean, block: () -> Unit) {
  val currentAutoAdvance = this.autoAdvance
  try {
    this.autoAdvance = value
    block()
  } finally {
    this.autoAdvance = currentAutoAdvance
  }
}

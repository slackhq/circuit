// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mirrors NavigableCircuitRetainedStateAndroidTest with first-party `retain {}` in the presenter
 * and [CircuitRetainedSettings.useFirstParty] enabled: retained values are scoped per record,
 * survive recreation while their record is in the nav stack, and are cleared when the record is
 * popped.
 */
@OptIn(ExperimentalCircuitRetainedApi::class)
@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitFirstPartyRetainAndroidTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<NavigableCircuitFirstPartyRetainTestActivity>()

  private val scenario: ActivityScenario<NavigableCircuitFirstPartyRetainTestActivity>
    get() = composeTestRule.activityRule.scenario

  @After
  fun tearDown() {
    CircuitRetainedSettings.useFirstParty = false
  }

  @Test
  fun retainScopedToBackstackWithRecreations() {
    composeTestRule.run {
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

      // Pop to Screen B. Increase count from 1 to 2.
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Navigate to Screen C. Assert that its state was not retained after the pop
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Pop to Screen B. Assert that its state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Pop to Screen A. Assert that its state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      scenario.recreate()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Assert that its state was not retained after the pop
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

/** Isolates push/pop (no recreation) for first-party retain scoped per record. */
@OptIn(ExperimentalCircuitRetainedApi::class)
@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitFirstPartyRetainPushPopTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<NavigableCircuitFirstPartyRetainTestActivity>()

  @After
  fun tearDown() {
    CircuitRetainedSettings.useFirstParty = false
  }

  @Test
  fun retainSurvivesPushAndPop() {
    composeTestRule.run {
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Push B, then pop back to A.
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
    }
  }
}

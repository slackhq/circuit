// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** Real-device smoke test for first-party retain scoped to navigation records. */
@OptIn(ExperimentalCircuitRetainedApi::class)
class NavigableCircuitFirstPartyRetainDeviceTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private val circuit =
    createTestCircuit(rememberType = TestCountPresenter.RememberType.FirstPartyRetain)

  init {
    CircuitRetainedSettings.useFirstParty = true
  }

  @After
  fun tearDown() {
    CircuitRetainedSettings.useFirstParty = false
  }

  @Test
  fun retainedValuesFollowRecordLifetimeAcrossRecreation() {
    setActivityContent()
    composeTestRule.run {
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      scenario.recreate()
      setActivityContent()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }

  private fun setActivityContent() {
    scenario.onActivity { activity ->
      activity.setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
          NavigableCircuitContent(navigator = navigator, backStack = backStack)
        }
      }
    }
  }
}

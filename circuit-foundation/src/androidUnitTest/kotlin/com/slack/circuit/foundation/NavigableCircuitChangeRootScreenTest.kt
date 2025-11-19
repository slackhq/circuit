// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.screen.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigableCircuitChangeRootScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun navigableCircuitContentUpdatesWhenRootScreenChanges() {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    var rootScreen by mutableStateOf<Screen>(TestScreen.ScreenA)
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(rootScreen)
        val navigator = rememberCircuitNavigator(navStack = backStack)
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
    composeTestRule.run {
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      // Navigate to Screen B
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")

      // Update root
      rootScreen = TestScreen.ScreenC

      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
    }
  }
}

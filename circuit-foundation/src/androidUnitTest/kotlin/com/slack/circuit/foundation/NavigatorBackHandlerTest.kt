// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

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
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.popUntil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigatorBackHandlerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun androidNavigatorRootPopBackHandler() {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack { push(TestScreen.ScreenA) }
        navigator = rememberCircuitNavigator(backStack = backStack)
        NavigableCircuitContent(navigator = navigator, backStack = backStack)
      }
    }
    composeTestRule.run {
      // Navigate to Screen B
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      // Navigate to Screen C
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      // Pop through the onRootPop into the back handler
      navigator.popUntil { false }
    }
  }
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestCountPresenter
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.popRoot
import com.slack.circuit.runtime.popUntil
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ComposeUiTestRunner::class)
class NavigatorBackHandlerTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun androidNavigatorRootPopBackHandler() {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    val rootCallback =
      object : OnBackPressedCallback(true) {
        var handled = 0

        override fun handleOnBackPressed() {
          handled++
        }
      }
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      val owner =
        requireNotNull(LocalOnBackPressedDispatcherOwner.current) {
          "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
        }
      DisposableEffect(rootCallback) {
        owner.onBackPressedDispatcher.addCallback(rootCallback)
        onDispose { rootCallback.remove() }
      }
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        navigator = rememberCircuitNavigator(navStack = backStack)
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
      waitForIdle()
      assertEquals(1, rootCallback.handled)
    }
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @Test
  fun nestedAndroidNavigatorRootPopBackHandler() {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    var outerBackCount = 0
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit) {
        BackHandler(enabled = true) { outerBackCount++ }
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        navigator = rememberCircuitNavigator(navStack = backStack)
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
      navigator.popRoot()
      waitForIdle()
      assertEquals(1, outerBackCount)
    }
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @Test
  fun nestedAndroidNavigatorRootDispatchedBackHandler() {
    val circuit = createTestCircuit(rememberType = TestCountPresenter.RememberType.Standard)
    var outerBackCount = 0
    lateinit var navigator: Navigator
    composeTestRule.setContent {
      CircuitCompositionLocals(circuit) {
        BackHandler(enabled = true) { outerBackCount++ }
        val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
        navigator = rememberCircuitNavigator(navStack = backStack)
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
      // Back through the onRootPop into the back handler
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
      waitForIdle()
      assertEquals(1, outerBackCount)
    }
  }
}

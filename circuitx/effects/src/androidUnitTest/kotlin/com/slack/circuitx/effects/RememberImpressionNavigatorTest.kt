// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.effects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.slack.circuit.retained.CanRetainChecker
import com.slack.circuit.retained.LocalCanRetainChecker
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val TAG_GOTO = "goto"
private const val TAG_POP = "pop"
private const val TAG_RESET = "reset"

@RunWith(RobolectricTestRunner::class)
class RememberImpressionNavigatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeNavigator = FakeNavigator(TestRootScreen)
  private val registry = RetainedStateRegistry()
  private val composed = MutableStateFlow(true)

  @Test
  fun recreateRememberImpressionNavigator() {
    composeTestRule.run {
      var count = 0
      // Compose the content
      setRetainedContent { RememberImpressionNavigatorContent(impression = { count++ }) }
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impression block was called once
      assertEquals(1, count)
      // Compose the content again
      recreate()
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impression block wasn't called again
      assertEquals(1, count)
    }
  }

  @Test
  fun inputChangeRememberImpressionNavigator() {
    composeTestRule.run {
      val inputs = MutableStateFlow("init")
      var count = 0
      // Compose the content
      setRetainedContent {
        val input by inputs.collectAsState()
        RememberImpressionNavigatorContent(input, impression = { count++ })
      }
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impression block was called once
      assertEquals(1, count)
      // Change the input
      inputs.value = "new"
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impression block was called again
      assertEquals(2, count)
    }
  }

  @Test
  fun navGoToRememberImpressionNavigator() {
    composeTestRule.run {
      var count = 0
      // Compose the content
      setRetainedContent { RememberImpressionNavigatorContent(impression = { count++ }) }
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impression block was called once
      assertEquals(1, count)
      // Fake navigation forward
      onNodeWithTag(TAG_GOTO).performClick()
      advanceTimeByAndRun(1)
      assertEquals(TestGoToScreen, fakeNavigator.takeNextScreen())
      // Compose the content again
      recreate()
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impression block was called again
      assertEquals(2, count)
    }
  }

  @Test
  fun navigatorDelegationRememberImpressionNavigator() = runTest {
    composeTestRule.run {
      var count = 0
      // Compose the content
      setRetainedContent { RememberImpressionNavigatorContent(impression = { count++ }) }
      // Simulate going to a screen
      onNodeWithTag(TAG_GOTO).performClick()
      advanceTimeByAndRun(1)
      fakeNavigator.awaitNextScreen()
      // Simulate a pop
      onNodeWithTag(TAG_POP).performClick()
      advanceTimeByAndRun(1)
      fakeNavigator.awaitPop()
      // Navigation reset
      onNodeWithTag(TAG_RESET).performClick()
      advanceTimeByAndRun(1)
      assertEquals(TestResetScreen, fakeNavigator.awaitResetRoot().newRoot)
    }
  }

  @Parcelize private data object TestRootScreen : Screen

  @Parcelize private data object TestGoToScreen : Screen

  @Parcelize private data object TestResetScreen : Screen

  @Composable
  private fun RememberImpressionNavigatorContent(vararg inputs: Any?, impression: () -> Unit) {
    val navigator =
      rememberImpressionNavigator(*inputs, navigator = fakeNavigator) {
        delay(1)
        impression()
      }
    Row {
      BasicText(
        text = "GoTo",
        modifier = Modifier.testTag(TAG_GOTO).clickable { navigator.goTo(TestGoToScreen) },
      )
      BasicText(text = "Pop", modifier = Modifier.testTag(TAG_POP).clickable { navigator.pop() })
      BasicText(
        text = "Reset",
        modifier = Modifier.testTag(TAG_RESET).clickable { navigator.resetRoot(TestResetScreen) },
      )
    }
  }

  private fun ComposeTestRule.advanceTimeByAndRun(milliseconds: Long) {
    mainClock.advanceTimeBy(milliseconds)
    mainClock.advanceTimeByFrame()
  }

  private fun ComposeContentTestRule.setRetainedContent(content: @Composable () -> Unit) {
    setContent {
      CompositionLocalProvider(
        LocalRetainedStateRegistry provides registry,
        LocalCanRetainChecker provides CanRetainChecker.Always,
      ) {
        val isCompose by composed.collectAsState(initial = true)
        if (isCompose) {
          content()
        }
      }
    }
  }

  private fun ComposeContentTestRule.recreate() {
    registry.saveAll()
    composed.value = false
    waitForIdle()
    composed.value = true
    waitForIdle()
  }
}

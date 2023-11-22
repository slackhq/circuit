// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.sideeffects

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.continuityRetainedStateRegistry
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

private const val TAG_GOTO = "goto"
private const val TAG_POP = "pop"
private const val TAG_RESET = "reset"

class ImpressionEffectTest {

  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  @Test
  fun recreateImpressionEffect() {
    composeTestRule.run {
      var count = 0
      val content = @Composable { ImpressionEffect { count++ } }
      // Compose the content
      setActivityContent(content)
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Recreate the activity
      scenario.recreate()
      // Compose the content again
      setActivityContent(content)
      // Verify the impresssion block wasn't called again
      assertEquals(1, count)
    }
  }

  @Test
  fun inputChangeImpressionEffect() {
    composeTestRule.run {
      val inputs = MutableStateFlow("init")
      var count = 0
      setActivityContent {
        val input by inputs.collectAsState()
        ImpressionEffect(input) { count++ }
      }
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Change the input
      inputs.value = "new"
      waitForIdle()
      // Verify the impresssion block was called again
      assertEquals(2, count)
    }
  }

  @Test
  fun recreateLaunchedImpressionEffect() {
    composeTestRule.run {
      var count = 0
      val content =
        @Composable {
          LaunchedImpressionEffect {
            delay(1)
            count++
          }
        }
      // Compose the content
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Recreate the activity
      scenario.recreate()
      // Compose the content again
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block wasn't called again
      assertEquals(1, count)
    }
  }

  @Test
  fun inputChangeLaunchedImpressionEffect() {
    composeTestRule.run {
      val inputs = MutableStateFlow("init")
      var count = 0
      // Compose the content
      setActivityContent {
        val input by inputs.collectAsState()
        LaunchedImpressionEffect(input) {
          delay(1)
          count++
        }
      }
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Change the input
      inputs.value = "new"
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called again
      assertEquals(2, count)
    }
  }

  @Test
  fun recreateRememberImpressionNavigator() {
    composeTestRule.run {
      val fakeNavigator = FakeNavigator()
      var count = 0
      val content =
        @Composable {
          RememberImpressionNavigatorContent(delegate = fakeNavigator, impression = { count++ })
        }
      // Compose the content
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Recreate the activity
      scenario.recreate()
      // Compose the content again
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block wasn't called again
      assertEquals(1, count)
    }
  }

  @Test
  fun inputChangeRememberImpressionNavigator() {
    composeTestRule.run {
      val inputs = MutableStateFlow("init")
      val fakeNavigator = FakeNavigator()
      var count = 0
      val content =
        @Composable {
          val input by inputs.collectAsState()
          RememberImpressionNavigatorContent(
            input,
            delegate = fakeNavigator,
            impression = { count++ }
          )
        }
      // Compose the content
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Change the input
      inputs.value = "new"
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called again
      assertEquals(2, count)
    }
  }

  @Test
  fun navGoToRememberImpressionNavigator() {
    composeTestRule.run {
      val fakeNavigator = FakeNavigator()
      var count = 0
      val content =
        @Composable {
          RememberImpressionNavigatorContent(delegate = fakeNavigator, impression = { count++ })
        }
      // Compose the content
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called once
      assertEquals(1, count)
      // Fake navigation forward
      onNodeWithTag(TAG_GOTO).performClick()
      advanceTimeByAndRun(1)
      assertEquals(TestGoToScreen, fakeNavigator.takeNextScreen())
      // Recreate the activity
      scenario.recreate()
      // Compose the content again
      setActivityContent(content)
      // Advance over the delay and execute
      advanceTimeByAndRun(1)
      // Verify the impresssion block was called again
      assertEquals(2, count)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun navigatorDelegationRememberImpressionNavigator() = runTest {
    composeTestRule.run {
      val fakeNavigator = FakeNavigator()
      var count = 0
      val content =
        @Composable {
          RememberImpressionNavigatorContent(delegate = fakeNavigator, impression = { count++ })
        }
      // Compose the content
      setActivityContent(content)
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
      assertEquals(TestResetScreen, fakeNavigator.awaitResetRoot())
    }
  }

  @Parcelize private data object TestGoToScreen : Screen

  @Parcelize private data object TestResetScreen : Screen

  @Composable
  private fun RememberImpressionNavigatorContent(
    vararg inputs: Any?,
    delegate: Navigator,
    impression: () -> Unit,
  ) {
    val navigator =
      rememberImpressionNavigator(*inputs, navigator = delegate) {
        delay(1)
        impression()
      }
    Row {
      BasicText(
        text = "GoTo",
        modifier = Modifier.testTag(TAG_GOTO).clickable { navigator.goTo(TestGoToScreen) },
      )
      BasicText(
        text = "Pop",
        modifier = Modifier.testTag(TAG_POP).clickable { navigator.pop() },
      )
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

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        CompositionLocalProvider(
          LocalRetainedStateRegistry provides continuityRetainedStateRegistry(),
        ) {
          content()
        }
      }
    }
  }
}

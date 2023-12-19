// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test

class NavigableCircuitRetainedStateAndroidTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>()

  private val scenario: ActivityScenario<TestActivity>
    get() = composeTestRule.activityRule.scenario

  @Test
  fun retainedStateScopedToBackstack() {
    composeTestRule.run {
      println("FOO Screen A")

      // Current: Screen A. Increase count to 1
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      println("FOO Screen B")

      // Navigate to Screen B. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      println("FOO Rotating Screen B")
      scenario.rotateAndBack()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      println("FOO Screen C")

      // Navigate to Screen C. Increase count to 1
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Now recreate the Activity and assert that the values were retained
      println("FOO Rotating Screen C")

      scenario.rotateAndBack()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      println("FOO Popping to Screen B")

      // Pop to Screen B. Increase count from 1 to 2.
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      println("FOO Screen B")

      onNodeWithTag(TAG_COUNT).assertTextEquals("1")
      onNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Navigate to Screen C. Assert that it's state was not retained
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Pop to Screen B. Assert that it's state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Pop to Screen A. Assert that it's state was retained
      onNodeWithTag(TAG_POP).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Assert that it's state was not retained
      onNodeWithTag(TAG_GO_NEXT).performClick()
      onNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }
}

@SuppressLint("SourceLockedOrientationActivity")
private fun ActivityScenario<out Activity>.rotateAndBack() {
  onActivity { activity ->
    val metrics = activity.resources.displayMetrics
    if (metrics.heightPixels > metrics.widthPixels) {
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
  }
  // Finally set the orientation back to unspecified
  //onActivity { activity ->
    //activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
 // }
}

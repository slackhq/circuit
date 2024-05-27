// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.ComponentActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags.TAG_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_INCREASE_COUNT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestContentTags.TAG_POP
import com.slack.circuit.internal.test.TestCountPresenter.RememberType
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

enum class GestureNavDecorationOption {
  AndroidPredictiveBack,
  Cupertino,
}

@OptIn(ExperimentalMaterialApi::class)
@Config(minSdk = 34)
@RunWith(ParameterizedRobolectricTestRunner::class)
class GestureNavigationStateTest(
  private val decorationOption: GestureNavDecorationOption,
  private val useKeys: Boolean,
  private val useSwipe: Boolean,
  private val rememberType: RememberType,
) {
  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(
      name = "{0}_useKeys={1}_useSwipe={2}_rememberType={3}"
    )
    fun params() =
      parameterizedParams()
        .combineWithParameters(
          GestureNavDecorationOption.Cupertino,
          GestureNavDecorationOption.AndroidPredictiveBack,
        )
        .combineWithParameters(true, false) // useKeys
        .combineWithParameters(true, false) // useSwipe
        .combineWithParameters(RememberType.Retained, RememberType.Saveable)
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun pop() {
    if (useSwipe) {
      when (decorationOption) {
        GestureNavDecorationOption.AndroidPredictiveBack -> {
          composeTestRule.activityRule.scenario.performGestureNavigationBackSwipe()
        }
        GestureNavDecorationOption.Cupertino -> {
          composeTestRule.onRoot().performTouchInput {
            swipeRight(startX = width * 0.2f, endX = width * 0.8f)
          }
        }
      }
    } else {
      composeTestRule.onTopNavigationRecordNodeWithTag(TAG_POP).performClick()
    }
  }

  @Test
  fun retainedStateScopedToBackstack() {
    composeTestRule.run {
      val circuit = createTestCircuit(useKeys = useKeys, rememberType = rememberType)

      setContent {
        CircuitCompositionLocals(circuit) {
          val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
          val navigator =
            rememberCircuitNavigator(
              backStack = backStack,
              onRootPop = {}, // no-op for tests
            )
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
            decoration =
              remember {
                when (decorationOption) {
                  GestureNavDecorationOption.AndroidPredictiveBack -> {
                    AndroidPredictiveBackNavigationDecoration(onBackInvoked = navigator::pop)
                  }
                  GestureNavDecorationOption.Cupertino -> {
                    CupertinoGestureNavigationDecoration(onBackInvoked = navigator::pop)
                  }
                }
              },
          )
        }
      }

      // Current: Screen A. Increase count to 1
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
      println("Increasing A")
      onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Increase count to 1
      println("Going to B")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
      println("Increasing B")
      onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen C. Increase count to 1
      println("Going to C")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
      println("Increasing C")
      onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Pop to Screen B. Increase count from 1 to 2.
      println("Pop to B")
      pop()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")
      println("Increasing B")
      onTopNavigationRecordNodeWithTag(TAG_INCREASE_COUNT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Navigate to Screen C. Assert that it's state was not retained
      println("Go to C")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("C")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")

      // Pop to Screen B. Assert that it's state was retained
      println("Popping to B")
      pop()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("2")

      // Pop to Screen A. Assert that it's state was retained
      println("Popping to A")
      pop()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("1")

      // Navigate to Screen B. Assert that it's state was not retained
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")
      onTopNavigationRecordNodeWithTag(TAG_COUNT).assertTextEquals("0")
    }
  }
}

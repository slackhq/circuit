// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.test.TestContentTags.TAG_GO_NEXT
import com.slack.circuit.internal.test.TestContentTags.TAG_LABEL
import com.slack.circuit.internal.test.TestScreen
import com.slack.circuit.internal.test.createTestCircuit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(minSdk = 34)
@RunWith(RobolectricTestRunner::class)
class GestureNavigationEventListenerTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val listener = RecordingGestureNavigationEventListener()

  @Test
  fun listenerObservesCompletedSwipe() {
    composeTestRule.run {
      setContent { TestContent() }

      // Navigate A -> B so there's something to pop.
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")
      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")

      activityRule.scenario.performGestureNavigationBackSwipe()
      waitForIdle()

      // The navigator drove the pop back to A.
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("A")

      // The listener observed the gesture lifecycle.
      assertTrue(listener.progressCount >= 1, "expected onBackProgress")
      assertEquals(1, listener.completedCount, "expected a single onBackCompleted")
      assertEquals(0, listener.cancelledCount, "expected no onBackCancelled")
    }
  }

  @Test
  fun listenerObservesCancelledSwipe() {
    composeTestRule.run {
      setContent { TestContent() }

      onTopNavigationRecordNodeWithTag(TAG_GO_NEXT).performClick()
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")

      activityRule.scenario.onActivity { activity ->
        val event =
          BackEventCompat(
            touchX = 0f,
            touchY = activity.window.decorView.height / 2f,
            progress = 0f,
            swipeEdge = BackEventCompat.EDGE_LEFT,
          )
        with(activity.onBackPressedDispatcher) {
          dispatchOnBackStarted(event)
          dispatchOnBackProgressed(event.copy(touchX = 10f, progress = 0.2f))
          dispatchOnBackProgressed(event.copy(touchX = 20f, progress = 0.4f))
          dispatchOnBackCancelled()
        }
      }
      waitForIdle()

      // The gesture was cancelled, so we stayed on B.
      onTopNavigationRecordNodeWithTag(TAG_LABEL).assertTextEquals("B")

      assertEquals(1, listener.cancelledCount, "expected a single onBackCancelled")
      assertEquals(0, listener.completedCount, "expected no onBackCompleted")
    }
  }

  @Composable
  private fun TestContent() {
    val circuit = createTestCircuit()
    CircuitCompositionLocals(circuit) {
      val backStack = rememberSaveableBackStack(TestScreen.ScreenA)
      val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
      NavigableCircuitContent(
        navigator = navigator,
        backStack = backStack,
        decoratorFactory = remember { AndroidPredictiveBackNavDecorator.Factory(listener) },
      )
    }
  }
}

private class RecordingGestureNavigationEventListener : GestureNavigationEventListener {
  var progressCount = 0
    private set

  var cancelledCount = 0
    private set

  var completedCount = 0
    private set

  override fun onBackProgress(progress: Float) {
    progressCount++
  }

  override fun onBackCancelled() {
    cancelledCount++
  }

  override fun onBackCompleted() {
    completedCount++
  }
}

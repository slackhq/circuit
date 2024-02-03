// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onFirst
import androidx.test.core.app.ActivityScenario
import com.slack.circuit.internal.test.TestContentTags

internal fun SemanticsNodeInteractionsProvider.onTopNavigationRecordNodeWithTag(
  testTag: String
): SemanticsNodeInteraction =
  onAllNodes(hasTestTag(testTag) and hasParent(hasTestTag(TestContentTags.TAG_ROOT)), false)
    // first is always on top
    .onFirst()

internal fun BackEventCompat.copy(
  touchX: Float = this.touchX,
  touchY: Float = this.touchY,
  progress: Float = this.progress,
  swipeEdge: Int = this.swipeEdge,
): BackEventCompat =
  BackEventCompat(touchX = touchX, touchY = touchY, progress = progress, swipeEdge = swipeEdge)

internal fun ActivityScenario<ComponentActivity>.performBackSwipeGesture() {
  onActivity { activity ->
    val event =
      BackEventCompat(
        touchX = 0f,
        touchY = activity.window.decorView.height / 2f,
        progress = 0f,
        swipeEdge = BackEventCompat.EDGE_LEFT,
      )

    with(activity.onBackPressedDispatcher) {
      dispatchOnBackStarted(event)
      // Now dispatch a series of back progress events up to progress == 1f
      dispatchOnBackProgressed(event.copy(touchX = 10f, progress = 0.2f))
      dispatchOnBackProgressed(event.copy(touchX = 20f, progress = 0.4f))
      dispatchOnBackProgressed(event.copy(touchX = 30f, progress = 0.6f))
      dispatchOnBackProgressed(event.copy(touchX = 40f, progress = 0.8f))
      dispatchOnBackProgressed(event.copy(touchX = 60f, progress = 1f))
      // Finally, trigger a 'back press' to finish the gesture
      onBackPressed()
    }
  }
}

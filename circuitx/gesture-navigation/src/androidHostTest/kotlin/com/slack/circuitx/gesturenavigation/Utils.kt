// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario

internal fun BackEventCompat.copy(
  touchX: Float = this.touchX,
  touchY: Float = this.touchY,
  progress: Float = this.progress,
  swipeEdge: Int = this.swipeEdge,
): BackEventCompat =
  BackEventCompat(touchX = touchX, touchY = touchY, progress = progress, swipeEdge = swipeEdge)

internal fun ActivityScenario<ComponentActivity>.performGestureNavigationBackSwipe() {
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

fun parameterizedParams(): List<Array<Any>> = emptyList()

inline fun <reified T> List<Array<T>>.combineWithParameters(vararg values: T): List<Array<T>> {
  if (isEmpty()) return values.map { arrayOf(it) }

  return fold(emptyList()) { acc, args ->
    val result = acc.toMutableList()
    values.forEach { result += (args + it) }
    result.toList()
  }
}

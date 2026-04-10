// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.slack.circuit.overlay.OverlayState.HIDDEN
import com.slack.circuit.overlay.OverlayState.SHOWING
import kotlin.test.Test
import org.junit.Rule

private const val OVERLAY_STATE_TAG = "overlay_state"

class OverlayUiTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun overlayStateLifecycle() {
    var shouldShow by mutableStateOf(false)
    compose.setContent {
      ContentWithOverlays {
        if (shouldShow) {
          OverlayEffect {
            show(TestOverlay())
            shouldShow = false
          }
        }
        Text(
          text = "State: ${LocalOverlayState.current}",
          modifier = Modifier.testTag(OVERLAY_STATE_TAG),
        )
      }
    }

    fun assertState(expected: OverlayState) {
      compose.onNodeWithTag(OVERLAY_STATE_TAG).assertTextContains("State: $expected")
    }

    repeat(3) {
      println("Iteration $it")
      // Initial state
      assertState(HIDDEN)

      // Show it
      shouldShow = true
      assertState(SHOWING)

      // Hide it again
      shouldShow = false
      assertState(HIDDEN)
    }
  }
}

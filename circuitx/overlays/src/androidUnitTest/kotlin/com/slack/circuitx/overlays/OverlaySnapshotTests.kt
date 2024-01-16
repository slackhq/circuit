// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziOptions.CompareOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayEffect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

const val SNAPSHOT_TAG = "snapshot_tag"

@OptIn(ExperimentalRoborazziApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = RobolectricDeviceQualifiers.Pixel7Pro)
class OverlaySnapshotTests {

  @get:Rule val composeTestRule = createComposeRule()

  private val roborazziOptions =
    RoborazziOptions(
      compareOptions =
        CompareOptions(changeThreshold = 0F, outputDirectoryPath = "src/androidUnitTest/snapshots"),
    )

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun alertDialogOverlay() {
    composeTestRule.run {
      setContent {
        MaterialTheme {
          ContentWithOverlays(Modifier.testTag(SNAPSHOT_TAG).fillMaxSize()) {
            Surface {
              OverlayEffect {
                it.show(
                  alertDialogOverlay(
                    confirmButton = { onClick -> Button(onClick) { Text("Confirm") } },
                    dismissButton = { onClick -> Button(onClick) { Text("Cancel") } },
                    title = { Text("Title") },
                    text = { Text("Text") },
                  )
                )
              }
            }
          }
        }
      }

      captureScreenRoboImage(roborazziOptions = roborazziOptions)
    }
  }

  @Test
  fun bottomSheetOverlay() {
    composeTestRule.run {
      setContent {
        MaterialTheme {
          ContentWithOverlays(Modifier.testTag(SNAPSHOT_TAG).fillMaxSize()) {
            Surface {
              OverlayEffect {
                it.show(
                  BottomSheetOverlay<String, Unit>(
                    model = "This is a bottom sheet",
                  ) { model, _ ->
                    Text(model)
                  }
                )
              }
            }
          }
        }
      }

      composeTestRule.waitForIdle()
      captureScreenRoboImage(roborazziOptions = roborazziOptions)
    }
  }
}

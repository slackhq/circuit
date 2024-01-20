// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziOptions.CompareOptions
import com.github.takahirom.roborazzi.RoborazziOptions.RecordOptions
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayEffect
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

const val SNAPSHOT_TAG = "snapshot_tag"

class OverlaySnapshotTests {

  @OptIn(
    ExperimentalTestApi::class,
    ExperimentalRoborazziApi::class,
    ExperimentalMaterial3Api::class
  )
  @Test
  fun alertDialog() = runDesktopComposeUiTest {
    setContent {
      MaterialTheme {
        ContentWithOverlays(Modifier.testTag(SNAPSHOT_TAG)) {
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

    val roborazziOptions =
      RoborazziOptions(
        recordOptions = RecordOptions(resizeScale = 0.5),
        compareOptions =
          CompareOptions(changeThreshold = 0F, outputDirectoryPath = "src/jvmTest/snapshots"),
      )

    waitForIdle()
    onNodeWithTag(SNAPSHOT_TAG).captureRoboImage(roborazziOptions = roborazziOptions)
  }
}

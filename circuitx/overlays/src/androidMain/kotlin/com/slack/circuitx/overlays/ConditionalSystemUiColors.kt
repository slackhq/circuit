// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * A [FullScreenOverlay.Callbacks] that remembers the current system UI colors and restores them
 * when finished.
 */
internal class ConditionalSystemUiColors(
  private val systemUiController: SystemUiController,
  initialStatusBarDarkContent: Boolean,
  initialNavBarDarkContent: Boolean,
) : FullScreenOverlay.Callbacks {
  private var storedStatusBarDarkContent by mutableStateOf(initialStatusBarDarkContent)
  private var storedNavBarDarkContent by mutableStateOf(initialNavBarDarkContent)

  override fun onShow() {
    storedStatusBarDarkContent = systemUiController.statusBarDarkContentEnabled
    storedNavBarDarkContent = systemUiController.navigationBarDarkContentEnabled
  }

  override fun onFinish() {
    systemUiController.statusBarDarkContentEnabled = storedStatusBarDarkContent
    systemUiController.navigationBarDarkContentEnabled = storedNavBarDarkContent
  }
}

// TODO if dark mode changes during this, it will restore the wrong colors. What do we do?
@Composable
internal fun rememberConditionalSystemUiColors(
  systemUiController: SystemUiController = rememberSystemUiController()
): ConditionalSystemUiColors {
  return ConditionalSystemUiColors(
    systemUiController,
    systemUiController.statusBarDarkContentEnabled,
    systemUiController.navigationBarDarkContentEnabled,
  )
}

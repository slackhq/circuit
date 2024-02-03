// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private class ConditionalSystemUiColorsImpl(
  private val systemUiController: SystemUiController,
  initialStatusBarDarkContent: Boolean,
  initialNavBarDarkContent: Boolean,
) : ConditionalSystemUiColors {
  private var storedStatusBarDarkContent by mutableStateOf(initialStatusBarDarkContent)
  private var storedNavBarDarkContent by mutableStateOf(initialNavBarDarkContent)

  override fun save() {
    storedStatusBarDarkContent = systemUiController.statusBarDarkContentEnabled
    storedNavBarDarkContent = systemUiController.navigationBarDarkContentEnabled
  }

  override fun restore() {
    systemUiController.statusBarDarkContentEnabled = storedStatusBarDarkContent
    systemUiController.navigationBarDarkContentEnabled = storedNavBarDarkContent
  }
}

@Composable
actual fun rememberConditionalSystemUiColors(): ConditionalSystemUiColors =
  rememberConditionalSystemUiColors(rememberSystemUiController())

// TODO if dark mode changes during this, it will restore the wrong colors. What do we do?
@Composable
private fun rememberConditionalSystemUiColors(
  systemUiController: SystemUiController
): ConditionalSystemUiColors {
  return ConditionalSystemUiColorsImpl(
    systemUiController,
    systemUiController.statusBarDarkContentEnabled,
    systemUiController.navigationBarDarkContentEnabled,
  )
}

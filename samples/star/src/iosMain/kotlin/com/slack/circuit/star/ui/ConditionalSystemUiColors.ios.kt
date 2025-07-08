// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.UIKit.UIApplication
import platform.UIKit.setStatusBarStyle

private class ConditionalSystemUiColorsImpl(private val application: UIApplication) :
  ConditionalSystemUiColors {
  private var storedStatusBarStyle by mutableLongStateOf(application.statusBarStyle)

  override fun save() {
    storedStatusBarStyle = application.statusBarStyle
  }

  override fun restore() {
    application.setStatusBarStyle(storedStatusBarStyle, animated = true)
  }
}

@Composable
actual fun rememberConditionalSystemUiColors(): ConditionalSystemUiColors {
  val application = UIApplication.sharedApplication

  return remember {
    if (application.statusBarHidden) {
      ConditionalSystemUiColors.None
    } else {
      ConditionalSystemUiColorsImpl(application = application)
    }
  }
}

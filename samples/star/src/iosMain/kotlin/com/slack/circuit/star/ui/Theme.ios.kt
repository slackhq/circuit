// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.setStatusBarStyle

@Composable
actual fun StarTheme(
  useDarkTheme: Boolean,
  // Dynamic color is not available on iOS in the same way as Android 12+
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) {
  val uiViewController = LocalUIViewController.current

  // iOS doesn't have dynamic color schemes like Android 12+, so we use static schemes
  val colorScheme = if (useDarkTheme) darkScheme else lightScheme

  SideEffect {
    val application = UIApplication.sharedApplication

    // Set status bar appearance based on theme
    val statusBarStyle =
      if (useDarkTheme) {
        UIStatusBarStyleLightContent // Light content on dark background
      } else {
        UIStatusBarStyleDarkContent // Dark content on light background
      }

    // Update status bar style
    application.setStatusBarStyle(statusBarStyle, animated = true)

    // Set the interface style on the view controller for system UI consistency
    uiViewController.overrideUserInterfaceStyle =
      if (useDarkTheme) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
      } else {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight
      }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}

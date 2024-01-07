// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    // No-op
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  @Composable
  actual fun isLandscape(): Boolean {
    return calculateWindowSizeClass().widthSizeClass > WindowWidthSizeClass.Compact
  }
}

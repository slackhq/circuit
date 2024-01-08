// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import android.content.res.Configuration
import androidx.activity.compose.ReportDrawnWhen as PlatformReportDrawnWhen
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    PlatformReportDrawnWhen(predicate)
  }

  @Composable
  actual fun isLandscape(): Boolean {
    return LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  }

  actual val type: PlatformType = PlatformType.ANDROID
}

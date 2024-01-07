// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.activity.compose.ReportDrawnWhen as PlatformReportDrawnWhen
import androidx.compose.runtime.Composable

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    PlatformReportDrawnWhen(predicate)
  }
}

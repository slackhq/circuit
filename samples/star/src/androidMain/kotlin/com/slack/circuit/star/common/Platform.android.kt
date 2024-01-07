// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.activity.compose.ReportDrawnWhen as PlatformReportDrawnWhen
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.slack.circuit.star.R

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    PlatformReportDrawnWhen(predicate)
  }

  @Composable
  actual fun appIconPainter(): Painter {
    return painterResource(R.drawable.star_icon)
  }
}

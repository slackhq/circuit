// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    // No-op
  }

  @Composable
  actual fun appIconPainter(): Painter {
    TODO("Not yet implemented")
  }
}

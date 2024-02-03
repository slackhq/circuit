// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo

actual object Platform {
  @Composable
  actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    // No-op
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @Composable
  actual fun isLandscape(): Boolean {
    val (w, h) = LocalWindowInfo.current.containerSize
    return w > h
  }

  actual val type: PlatformType = PlatformType.JVM
}

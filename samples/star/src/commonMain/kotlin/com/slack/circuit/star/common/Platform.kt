// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.runtime.Composable

expect object Platform {
  @Composable fun ReportDrawnWhen(predicate: () -> Boolean)

  @Composable fun isLandscape(): Boolean

  val type: PlatformType
}

enum class PlatformType {
  JVM,
  ANDROID,
  IOS,
}

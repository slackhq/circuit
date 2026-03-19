// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo

expect object Platform {
  @Composable fun ReportDrawnWhen(predicate: () -> Boolean)

  val type: PlatformType
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun isLandscape(): Boolean {
  val (w, h) = LocalWindowInfo.current.containerSize
  return w > h
}

enum class PlatformType {
  JVM,
  ANDROID,
  IOS,
}

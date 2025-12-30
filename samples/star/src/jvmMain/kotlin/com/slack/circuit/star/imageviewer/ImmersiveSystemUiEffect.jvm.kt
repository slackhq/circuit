// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.compose.runtime.Composable

@Composable
actual fun ImmersiveSystemUiEffect(showChrome: Boolean) {
  // No-op on JVM - desktop doesn't have system bars to hide
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformCompositionLocals(content: @Composable () -> Unit) {
  // No JVM-specific locals currently!
  content()
}

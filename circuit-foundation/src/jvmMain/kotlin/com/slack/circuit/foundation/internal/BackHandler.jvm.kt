// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable

@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  // No-op
}

@Composable
internal actual fun BackHandlerBox(content: @Composable () -> Unit) {
  Box { content() }
}

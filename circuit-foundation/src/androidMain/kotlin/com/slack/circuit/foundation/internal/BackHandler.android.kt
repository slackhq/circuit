// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as ActivityBackHandler

@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  ActivityBackHandler(enabled = enabled, onBack = onBack)
}

@Composable
internal actual fun BackHandlerBox(content: @Composable () -> Unit) {
  Box { content() }
}

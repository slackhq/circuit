// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.activity.compose.BackHandler as ActivityBackHandler
import androidx.compose.runtime.Composable

@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  ActivityBackHandler(enabled = enabled, onBack = onBack)
}

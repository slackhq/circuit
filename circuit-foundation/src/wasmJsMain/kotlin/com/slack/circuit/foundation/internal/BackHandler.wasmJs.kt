// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable

/**
 * A multiplatform abstraction over a BackHandler that's only implemented on Android and a no-op
 * elsewhere.
 */
@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  // No-op
}

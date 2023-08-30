// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Checks whether or not we can retain in the current composable context. */
@Composable
public actual fun rememberCanRetainChecker(): CanRetainChecker {
  return remember { CanRetainChecker { false } }
}

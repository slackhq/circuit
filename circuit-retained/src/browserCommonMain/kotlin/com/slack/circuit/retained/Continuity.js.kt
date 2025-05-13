// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

@Composable
public actual fun continuityRetainedStateRegistry(key: String): RetainedStateRegistry {
  return rememberRetainedStateRegistry(key = key, canRetainChecker = CanRetainChecker.Always)
}

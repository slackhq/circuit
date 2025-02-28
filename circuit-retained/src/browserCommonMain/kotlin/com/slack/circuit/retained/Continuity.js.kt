// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

@Composable
public actual fun continuityRetainedStateRegistry(
  key: String,
  canRetainChecker: CanRetainChecker,
): RetainedStateRegistry {
  return rememberRetainedStateRegistry(key = key, canRetainChecker = canRetainChecker)
}

/** Checks whether or not we can retain in the current composable context. */
@Composable
public actual fun rememberContinuityCanRetainChecker(): CanRetainChecker {
  return CanRetainChecker.Always
}

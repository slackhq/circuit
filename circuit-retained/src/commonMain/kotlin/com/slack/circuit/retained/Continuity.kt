// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

/** Provides a [RetainedStateRegistry]. */
@Composable
public expect fun continuityRetainedStateRegistry(
  key: String = Continuity.KEY,
): RetainedStateRegistry

public object Continuity {
  public const val KEY: String = "CircuitContinuity"
}

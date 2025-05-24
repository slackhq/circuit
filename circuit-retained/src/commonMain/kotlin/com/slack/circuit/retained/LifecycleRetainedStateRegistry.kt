// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

@Composable
public expect fun lifecycleRetainedStateRegistry(
  key: String = LifecycleRetainedStateRegistry.KEY
): RetainedStateRegistry

public object LifecycleRetainedStateRegistry {
  public const val KEY: String = "CircuitLifecycleRetainedStateRegistry"
}

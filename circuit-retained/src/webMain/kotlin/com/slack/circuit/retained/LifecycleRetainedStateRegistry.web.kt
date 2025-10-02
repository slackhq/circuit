// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable

@Composable
public actual fun lifecycleRetainedStateRegistry(key: String): RetainedStateRegistry =
  viewModelRetainedStateRegistry(key, RetainedStateRegistryViewModel.Factory)

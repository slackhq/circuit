// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION")

package com.slack.circuit.retained

import androidx.compose.runtime.Composable

@Composable
@Deprecated(RETAINED_STATE_REGISTRY_DEPRECATION)
public actual fun lifecycleRetainedStateRegistry(key: String): RetainedStateRegistry =
  rememberRetainedStateRegistry(key = key)

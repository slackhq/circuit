// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.compose.runtime.Composable

/**
 * Platform-specific effect for managing system UI visibility in an immersive image viewer.
 * On Android, this hides/shows the system bars based on [showChrome].
 * On other platforms, this is a no-op.
 */
@Composable
expect fun ImmersiveSystemUiEffect(showChrome: Boolean)

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.internal

import androidx.compose.runtime.Composable

/**
 * A multiplatform abstraction over a BackHandler that's only implemented on Android and a no-op
 * elsewhere.
 */
@Composable public expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

@Composable internal expect fun BackHandlerBox(content: @Composable () -> Unit)

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.ui

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf

/**
 * A composition local for accessing [WindowWidthSizeClass]. This API is unfortunately tied to
 * activities, so this felt like the less bad option for exposing it to Circuit UIs that try not to
 * know anything about activities.
 */
val LocalWindowWidthSizeClass = compositionLocalOf { WindowWidthSizeClass.Compact }

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.star.imageviewer.ImageViewerScreen.State
import dev.zacsweers.metro.AppScope

@CircuitInject(ImageViewerScreen::class, AppScope::class)
@Composable
actual fun ImageViewer(state: State, modifier: Modifier) {
  BasicImageViewer(state, modifier)
}

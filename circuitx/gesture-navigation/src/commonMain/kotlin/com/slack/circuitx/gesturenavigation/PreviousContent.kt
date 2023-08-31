// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout

@Composable
internal fun PreviousContent(
  isVisible: () -> Boolean = { true },
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier
      // If we're not visible, don't measure, layout (or draw)
      .let { if (isVisible()) it else it.emptyLayout() }
      // Content in the back stack should not be interactive until they're on top
      .pointerInput(Unit) {},
  ) {
    content()
  }
}

/** This no-ops measure + layout (and thus draw) for child content. */
private fun Modifier.emptyLayout(): Modifier = layout { _, constraints ->
  layout(constraints.minWidth, constraints.minHeight) {}
}

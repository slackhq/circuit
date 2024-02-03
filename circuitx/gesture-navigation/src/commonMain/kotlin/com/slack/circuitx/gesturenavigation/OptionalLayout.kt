// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout

/**
 * A composable which optionally lays out the given [content], depending on what [shouldLayout]
 * returns, whilst still keeping [content] attached to composition.
 *
 * This is useful when used in conjunction with `movableContentOf`, as that will destroy the
 * content's state 'very soon' after the content is detached from composition. By using this
 * composable, the content can remain attached to composition, but can avoid being measured, laid
 * out or drawn, allowing us to reduce wasted work.
 */
@Composable
internal fun OptionalLayout(
  shouldLayout: () -> Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier
      // If we're not visible, don't measure, layout (or draw)
      .let { if (shouldLayout()) it else it.emptyLayout() }
      // Content in the back stack should not be interactive until they're on top
      .pointerInput(Unit) {}
  ) {
    content()
  }
}

/** This no-ops measure + layout (and thus draw) for child content. */
private fun Modifier.emptyLayout(): Modifier = layout { _, constraints ->
  layout(constraints.minWidth, constraints.minHeight) {}
}

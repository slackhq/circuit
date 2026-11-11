// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.roundToInt

/**
 * Gives a list the width that [ListDetailPaneStrategy] gives a list pane: [fraction] of the maximum
 * incoming width, limited to [widthRange].
 *
 * Use it in a left pane that sizes itself, on the part that holds the list. In a row, the maximum
 * incoming width is the width that the parts before it left, so a list after a navigation rail gets
 * a share of the width beside the rail.
 *
 * The incoming constraints still win. Under a fixed width, like the full window, this changes
 * nothing.
 *
 * @see PaneSplit.PrimaryWraps
 */
@ExperimentalCircuitAdaptiveApi
public fun Modifier.listPaneWidth(
  fraction: Float = ListDetailPaneDefaults.ListPaneFraction,
  widthRange: ClosedRange<Dp> = ListDetailPaneDefaults.ListPaneWidthRange,
): Modifier {
  return layout { measurable, constraints ->
    val preferred =
      if (constraints.hasBoundedWidth) {
        (constraints.maxWidth * fraction)
          .roundToInt()
          .coerceIn(widthRange.start.roundToPx(), widthRange.endInclusive.roundToPx())
      } else {
        widthRange.endInclusive.roundToPx()
      }
    val width = constraints.constrainWidth(preferred)
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
  }
}

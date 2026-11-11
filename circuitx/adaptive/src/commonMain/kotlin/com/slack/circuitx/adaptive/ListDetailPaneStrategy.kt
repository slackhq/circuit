// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.Screen

/** How a screen shares a wide window with the screen below it. */
@ExperimentalCircuitAdaptiveApi
public enum class PaneStyle {
  /** A list's detail. The list keeps a narrow pane on the left and the detail fills the rest. */
  LIST_DETAIL,

  /** Extra information about the screen below, which keeps most of the width. */
  SUPPORTING,
}

/**
 * A screen that opens beside the screen below it on wide windows, in the way its [paneStyle]
 * describes. Screens pushed over it open in the same pane.
 *
 * This marker is the default lookup of [ListDetailPaneStrategy]. Pass another lookup to keep screen
 * classes free of this type.
 */
@ExperimentalCircuitAdaptiveApi
public interface PaneScreen : Screen {
  public val paneStyle: PaneStyle
}

/** The default pane sizes of [ListDetailPaneStrategy]. */
@ExperimentalCircuitAdaptiveApi
public object ListDetailPaneDefaults {
  /** The left pane's share of the width for [PaneStyle.LIST_DETAIL]. */
  public const val ListPaneFraction: Float = 0.4f

  /** Limits of the left pane's width for [PaneStyle.LIST_DETAIL]. */
  public val ListPaneWidthRange: ClosedRange<Dp> = 280.dp..400.dp

  /** The right pane's share of the width for [PaneStyle.SUPPORTING]. */
  public const val SupportingPaneFraction: Float = 0.33f

  /** Limits of the right pane's width for [PaneStyle.SUPPORTING]. */
  public val SupportingPaneWidthRange: ClosedRange<Dp> = 320.dp..400.dp

  /** The narrowest pane to show on either side of a hinge. */
  public val MinHingePaneWidth: Dp = 280.dp
}

/**
 * Shows a screen beside the screens that open from it, on windows at least [minWidthDp] wide and
 * [minHeightDp] tall. The defaults are Material's expanded width and medium height, which most
 * tablets reach in landscape.
 *
 * The left pane takes the record just below the topmost run of records that have a [PaneStyle]. For
 * the stack `Detail, List, Home` where only `Detail` has a style, `List` takes the left pane and
 * `Home` stays hidden. The run's bottom record picks the style for the whole run.
 *
 * A root that is alone on the stack stays in the left pane, so it can reserve the right pane with
 * [PaneHost.placeholder] before the first pick. A stack whose top record has no style, and is not
 * the root, shows only that record.
 *
 * The panes split at the first hinge when [minHingePaneWidth] fits on both sides of it.
 *
 * @param paneStyle Returns the style of a screen, or null when it has none.
 * @param listPaneFraction The left pane's share of the width for [PaneStyle.LIST_DETAIL].
 * @param listPaneWidthRange Limits of the left pane's width for [PaneStyle.LIST_DETAIL].
 * @param listPaneSizesItself Lets the left pane of [PaneStyle.LIST_DETAIL] take the width its
 *   content asks for, in place of [listPaneFraction] and [listPaneWidthRange]. See
 *   [PaneSplit.PrimaryWraps].
 * @param supportingPaneFraction The right pane's share of the width for [PaneStyle.SUPPORTING].
 * @param supportingPaneWidthRange Limits of the right pane's width for [PaneStyle.SUPPORTING].
 * @param placeholder Draws the right pane while no record is open there, or null to give the left
 *   pane the full width.
 */
@ExperimentalCircuitAdaptiveApi
public class ListDetailPaneStrategy(
  private val paneStyle: (Screen) -> PaneStyle? = { (it as? PaneScreen)?.paneStyle },
  private val minWidthDp: Int = WIDTH_DP_EXPANDED_LOWER_BOUND,
  private val minHeightDp: Int = HEIGHT_DP_MEDIUM_LOWER_BOUND,
  private val listPaneFraction: Float = ListDetailPaneDefaults.ListPaneFraction,
  private val listPaneWidthRange: ClosedRange<Dp> = ListDetailPaneDefaults.ListPaneWidthRange,
  private val listPaneSizesItself: Boolean = false,
  private val supportingPaneFraction: Float = ListDetailPaneDefaults.SupportingPaneFraction,
  private val supportingPaneWidthRange: ClosedRange<Dp> =
    ListDetailPaneDefaults.SupportingPaneWidthRange,
  private val minHingePaneWidth: Dp = ListDetailPaneDefaults.MinHingePaneWidth,
  private val placeholder: (@Composable () -> Unit)? = null,
) : PaneStrategy {

  override fun scene(window: PaneWindow, stack: NavStackList<out NavArgument>): PaneScene? {
    if (!window.sizeClass.isAtLeastBreakpoint(minWidthDp, minHeightDp)) {
      return null
    }
    // Top first, root last. Forward items stay hidden, as Circuit shows the active record.
    val visible = listOf(stack.active) + stack.backwardItems
    // The root always has a record to sit beside, so it never counts as part of the run.
    val runLength = visible.dropLast(1).takeWhile { paneStyle(it.screen) != null }.size
    if (runLength == 0 && visible.size > 1) {
      return null
    }
    val width = window.size.width
    val style =
      if (runLength == 0) {
        // The panes stay in place so the root can reserve the right pane without a layout switch.
        PaneStyle.LIST_DETAIL
      } else {
        checkNotNull(paneStyle(visible[runLength - 1].screen))
      }
    val hinge =
      window.separatingHinges.firstOrNull()?.takeIf {
        it.left >= minHingePaneWidth && width - it.right >= minHingePaneWidth
      }
    val split =
      when {
        hinge != null -> PaneSplit.AtHinge
        style == PaneStyle.SUPPORTING -> {
          PaneSplit.Fixed(
            width -
              (width * supportingPaneFraction).coerceIn(
                supportingPaneWidthRange.start,
                supportingPaneWidthRange.endInclusive,
              )
          )
        }
        listPaneSizesItself -> PaneSplit.PrimaryWraps()
        // The detail gets most of the width, so a grid beside the list keeps room for its columns.
        else -> {
          PaneSplit.Fixed(
            (width * listPaneFraction).coerceIn(
              listPaneWidthRange.start,
              listPaneWidthRange.endInclusive,
            )
          )
        }
      }
    return PaneScene(
      primaryKey = visible[runLength].key,
      split = split,
      placeholder = placeholder,
    )
  }
}

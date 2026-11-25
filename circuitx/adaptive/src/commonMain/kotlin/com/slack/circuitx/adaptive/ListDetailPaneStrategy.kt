// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.Screen

/** How a screen shares a wide window with the screens around it. */
@ExperimentalCircuitAdaptiveApi
public enum class PaneStyle {
  /**
   * A screen that opens other screens beside it, like a list. It takes the start pane while it is
   * the top screen, so it can show a placeholder before the first pick. The root needs no style for
   * this. Use it for a list that opens over other screens.
   */
  LIST,

  /** A list's detail. The list keeps a narrow pane at the start and the detail fills the rest. */
  LIST_DETAIL,

  /** Extra information about the screen below, which keeps most of the width. */
  SUPPORTING,

  /**
   * More content for an open detail, like a profile opened from a conversation. A window with room
   * for three panes shows it in a third pane, beside the list and the detail. Other windows stack
   * it over the detail.
   */
  EXTRA,
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

/**
 * Shows a screen beside the screens that open from it, on windows with room for two panes
 * ([PaneWindow.maxPanes]) that are at least [minHeightDp] tall. With the default [PaneWindow], that
 * is Material's expanded width, which most tablets reach in landscape.
 *
 * The start pane takes the record just below the topmost run of records that have a [PaneStyle].
 * For the stack `Detail, List, Home` where only `Detail` has a style, `List` takes the start pane
 * and `Home` stays hidden. The run's bottom record picks the style for the whole run.
 *
 * A top record with no screens open beside it stays in the start pane when it is the root, or when
 * it has [PaneStyle.LIST]. It can then reserve the end pane with [PaneHost.placeholder] before the
 * first pick, and its pane resizes with an animation on that pick. Any other top record with no
 * pane style fills the window. Set [rootIsList] to false when the root is not always a list, and
 * give each list [PaneStyle.LIST].
 *
 * On a window with room for three panes, a [PaneStyle.EXTRA] record above a list's detail opens a
 * third pane. The record just below the lowest such record takes the middle pane.
 *
 * The panes split at the first hinge when [minHingePaneWidth] fits on both sides of it. A window
 * with a hinge shows two panes at most.
 *
 * Pane widths default to [PaneWindow.preferredPaneWidth], which is Material's pane width for the
 * default [PaneWindow]. Each width is a function of the window, so it can follow the width that the
 * panes share:
 * ```kotlin
 * ListDetailPaneStrategy(listPaneWidth = { (it.size.width * 0.4f).coerceIn(280.dp, 400.dp) })
 * ```
 *
 * @param paneStyle Returns the style of a screen, or null when it has none.
 * @param minHeightDp The shortest window that splits. The default is the medium height class, which
 *   keeps phones in landscape on one pane.
 * @param listPaneWidth Returns the width of the start pane for [PaneStyle.LIST_DETAIL].
 * @param listPaneSizesItself Lets the start pane of [PaneStyle.LIST_DETAIL] take the width its
 *   content asks for, in place of [listPaneWidth]. See [PaneSplit.PrimaryWraps].
 * @param supportingPaneWidth Returns the width of the end pane for [PaneStyle.SUPPORTING].
 * @param extraPaneWidth Returns the width of the third pane for [PaneStyle.EXTRA].
 * @param minHingePaneWidth The narrowest pane to show on either side of a hinge.
 * @param rootIsList Treats a root with no pane style as a [PaneStyle.LIST] screen.
 * @param placeholder Draws the end pane while no record is open there, or null to give the start
 *   pane the full width. It gets the screen of the start pane, so one strategy can draw a different
 *   placeholder for each list.
 */
@ExperimentalCircuitAdaptiveApi
public class ListDetailPaneStrategy(
  private val paneStyle: (Screen) -> PaneStyle? = { (it as? PaneScreen)?.paneStyle },
  private val minHeightDp: Int = HEIGHT_DP_MEDIUM_LOWER_BOUND,
  private val listPaneWidth: (PaneWindow) -> Dp = { it.preferredPaneWidth },
  private val listPaneSizesItself: Boolean = false,
  private val supportingPaneWidth: (PaneWindow) -> Dp = { it.preferredPaneWidth },
  private val extraPaneWidth: (PaneWindow) -> Dp = { it.preferredPaneWidth },
  private val minHingePaneWidth: Dp = 0.dp,
  private val rootIsList: Boolean = true,
  private val placeholder: (@Composable (primary: Screen) -> Unit)? = null,
) : PaneStrategy {

  override fun scene(window: PaneWindow, stack: NavStackList<out NavArgument>): PaneScene? {
    if (window.maxPanes < 2 || !window.sizeClass.isHeightAtLeastBreakpoint(minHeightDp)) {
      return null
    }
    // Top first, root last. Forward items stay hidden, as Circuit shows the active record.
    val visible = listOf(stack.active) + stack.backwardItems
    // The root always has a record to sit beside, so it never counts as part of the run. A list
    // ends the run, because the screens above it open beside it.
    val runLength =
      visible
        .dropLast(1)
        .takeWhile { record ->
          val recordStyle = paneStyle(record.screen)
          recordStyle != null && recordStyle != PaneStyle.LIST
        }
        .size
    if (runLength == 0) {
      // Nothing is open beside the top record. Only a list keeps the panes.
      val topStyle = paneStyle(visible[0].screen)
      val topIsList = topStyle == PaneStyle.LIST || (visible.size == 1 && rootIsList)
      if (!topIsList) {
        return null
      }
    }
    val width = window.size.width
    val style =
      if (runLength == 0) {
        // The panes stay in place so the root can reserve the end pane without a layout switch.
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
          PaneSplit.EndFixed(supportingPaneWidth(window))
        }
        listPaneSizesItself -> PaneSplit.PrimaryWraps()
        else -> PaneSplit.Fixed(listPaneWidth(window))
      }
    // The record below the lowest EXTRA record of the run, when the window fits a third pane. A
    // supporting pane already takes its width from the main pane, so it gets no third pane. A
    // window with any hinge gets none either, also when the hinge is too close to an edge to split
    // at, because a middle pane would cross it.
    val secondaryIndex =
      if (
        window.maxPanes >= 3 && window.separatingHinges.isEmpty() && style != PaneStyle.SUPPORTING
      ) {
        (runLength - 2 downTo 0)
          .firstOrNull { paneStyle(visible[it].screen) == PaneStyle.EXTRA }
          ?.let { it + 1 }
      } else {
        null
      }
    return PaneScene(
      primaryKey = visible[runLength].key,
      split = split,
      placeholder = placeholder,
      secondaryKey = secondaryIndex?.let { visible[it].key },
      endPaneWidth =
        if (secondaryIndex != null) {
          extraPaneWidth(window)
        } else {
          Dp.Unspecified
        },
    )
  }
}

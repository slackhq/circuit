// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.Screen

/** Marker interface for screens that act as the list pane in a [ListDetailNavStage]. */
@ExperimentalNavStageApi public interface ListPane

/** Marker interface for screens that act as the detail pane in a [ListDetailNavStage]. */
@ExperimentalNavStageApi public interface DetailPane

/**
 * Strategy that activates [ListDetailNavStage] when [isMultiPane] is true, the active screen is a
 * detail pane, and the back stack contains a list pane.
 *
 * [isMultiPane] defaults to a medium-or-wider window width. Override it to supply your own
 * breakpoint, which also avoids pulling in the default window-size-class dependency.
 */
@ExperimentalNavStageApi
public class ListDetailNavStageStrategy(
  private val isListPane: (Screen) -> Boolean = { it is ListPane },
  private val isDetailPane: (Screen) -> Boolean = { it is DetailPane },
  private val isMultiPane: @Composable () -> Boolean = { DefaultIsMultiPane() },
  private val listTransition: (Screen) -> PaneTransition = { PaneTransition.None },
  private val detailTransition: (Screen) -> PaneTransition = { PaneTransition.Default },
) : NavStageStrategy {

  @Composable
  override fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>? {
    if (!isMultiPane()) return null
    if (!isDetailPane(args.active.screen)) return null
    if (args.backwardItems.none { isListPane(it.screen) }) return null
    // Remembered so the stage keeps a stable identity while this layout is in use.
    return remember(isListPane, listTransition, detailTransition) {
      ListDetailNavStage(
        isListPane = isListPane,
        listTransition = listTransition,
        detailTransition = detailTransition,
      )
    }
  }

  public companion object {
    /** The default multi-pane gate: any window at least [WindowWidthSizeClass.Medium] wide. */
    @Composable
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    public fun DefaultIsMultiPane(): Boolean =
      calculateWindowSizeClass().widthSizeClass != WindowWidthSizeClass.Compact
  }
}

/**
 * Dual-pane stage that renders a list pane (40%) beside a detail pane (60%) in a horizontal row.
 *
 * Falls back to rendering only the active item when the stack holds no list pane, which happens
 * while a transition renders an older stack than the one this stage was resolved from.
 */
@ExperimentalNavStageApi
public class ListDetailNavStage<T : NavArgument>(
  private val isListPane: (Screen) -> Boolean,
  private val listTransition: (Screen) -> PaneTransition = { PaneTransition.None },
  private val detailTransition: (Screen) -> PaneTransition = { PaneTransition.Default },
) : NavStage<T> {
  override val key: Any = STAGE_KEY

  override fun visibleItems(args: NavStackList<T>): List<T> {
    val listItem =
      args.backwardItems.firstOrNull { isListPane(it.screen) } ?: return listOf(args.active)
    return listOf(listItem, args.active)
  }

  @Composable
  override fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    val items = visibleItems(args)
    if (items.size < 2) {
      val detailItem = items.single()
      Box(modifier.fillMaxSize()) {
        paneScope.Pane(
          key = DETAIL_PANE_KEY,
          item = detailItem,
          transition = detailTransition(detailItem.screen),
        )
      }
      return
    }
    val (listItem, detailItem) = items
    Row(modifier.fillMaxSize()) {
      paneScope.Pane(
        key = LIST_PANE_KEY,
        item = listItem,
        modifier = Modifier.weight(0.4f),
        transition = listTransition(listItem.screen),
      )
      paneScope.Pane(
        key = DETAIL_PANE_KEY,
        item = detailItem,
        modifier = Modifier.weight(0.6f),
        transition = detailTransition(detailItem.screen),
      )
    }
  }

  private companion object {
    const val STAGE_KEY = "com.slack.circuitx.navstage.list-detail"
    const val LIST_PANE_KEY = "com.slack.circuitx.navstage.list-detail.list"
    const val DETAIL_PANE_KEY = "com.slack.circuitx.navstage.list-detail.detail"
  }
}

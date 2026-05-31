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
 * Strategy that activates [ListDetailNavStage] on medium+ width screens when the active screen is a
 * detail pane and the back stack contains a list pane.
 */
@ExperimentalNavStageApi
public class ListDetailNavStageStrategy(
  private val isListPane: (Screen) -> Boolean = { it is ListPane },
  private val isDetailPane: (Screen) -> Boolean = { it is DetailPane },
  private val listTransition: (Screen) -> PaneTransition = { PaneTransition.None },
  private val detailTransition: (Screen) -> PaneTransition = { PaneTransition.Default },
) : NavStageStrategy {

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  @Composable
  override fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>? {
    val windowSizeClass = calculateWindowSizeClass()
    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) return null

    val hasDetail = isDetailPane(args.active.screen)
    val hasList = args.backwardItems.any { isListPane(it.screen) }
    if (!hasDetail || !hasList) return null

    return remember(isListPane, listTransition, detailTransition) {
      ListDetailNavStage(
        isListPane = isListPane,
        listTransition = listTransition,
        detailTransition = detailTransition,
      )
    }
  }
}

/**
 * Dual-pane stage that renders a list pane (40%) beside a detail pane (60%) in a horizontal row.
 */
@ExperimentalNavStageApi
public class ListDetailNavStage<T : NavArgument>(
  private val isListPane: (Screen) -> Boolean,
  private val listTransition: (Screen) -> PaneTransition = { PaneTransition.None },
  private val detailTransition: (Screen) -> PaneTransition = { PaneTransition.Default },
) : NavStage<T> {
  override val key: Any = "list-detail"

  override fun renderedItemKeys(args: NavStackList<T>): Set<Any> {
    val listKey = args.backwardItems.firstOrNull { isListPane(it.screen) }?.key
    return setOfNotNull(args.active.key, listKey)
  }

  @Composable
  override fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    val detailItem = args.active
    // Find the nearest list pane behind the current detail.
    // If multiple list panes exist, we want the most recently pushed one.
    val listItem = args.backwardItems.firstOrNull { isListPane(it.screen) }
    if (listItem == null) {
      // Fallback: render only the detail pane until the next recomposition picks a new stage
      Box(modifier.fillMaxSize()) {
        paneScope.Pane(key = "detail", item = detailItem, transition = detailTransition(detailItem.screen))
      }
      return
    }

    Row(modifier.fillMaxSize()) {
      paneScope.Pane(key = "list", item = listItem, modifier = Modifier.weight(0.4f), transition = listTransition(listItem.screen))
      paneScope.Pane(key = "detail", item = detailItem, modifier = Modifier.weight(0.6f), transition = detailTransition(detailItem.screen))
    }
  }
}

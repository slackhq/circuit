// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
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
) : NavStageStrategy {

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  @Composable
  override fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>? {
    val windowSizeClass = calculateWindowSizeClass()
    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) return null

    val hasDetail = isDetailPane(args.active.screen)
    val hasList = args.backwardItems.any { isListPane(it.screen) }
    if (!hasDetail || !hasList) return null

    return ListDetailNavStage(isListPane = isListPane)
  }
}

/**
 * Dual-pane stage that renders a list pane (40%) beside a detail pane (60%) in a horizontal row.
 */
@ExperimentalNavStageApi
public class ListDetailNavStage<T : NavArgument>(private val isListPane: (Screen) -> Boolean) :
  NavStage<T> {
  override val key: Any = "list-detail"

  @Composable
  override fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    val detailItem = args.active
    val listItem = args.backwardItems.first { isListPane(it.screen) }

    // TODO Problems
    //   - If the list item moves between here and Single it blows up (key was used multiple times)
    //   - This needs to animate content panes in/out, can use the Compose component for this too.
    Row(modifier.fillMaxSize()) {
      paneScope.Pane(key = "list", item = listItem, modifier = Modifier.weight(0.4f))
      paneScope.Pane(key = "detail", item = detailItem, modifier = Modifier.weight(0.6f))
    }
  }
}

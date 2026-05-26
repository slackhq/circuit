// Copyright (C) 2025 Slack Technologies, LLC
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

@ExperimentalNavStageApi
public class ListDetailNavStage<T : NavArgument>(
  private val isListPane: (Screen) -> Boolean,
) : NavStage<T> {
  override val key: Any = "list-detail"

  @Composable
  override fun Content(
    args: NavStackList<T>,
    paneScope: NavStagePaneScope<T>,
    modifier: Modifier,
  ) {
    val detailItem = args.active
    val listItem = args.backwardItems.first { isListPane(it.screen) }

    Row(modifier.fillMaxSize()) {
      paneScope.Pane(
        key = "list",
        item = listItem,
        modifier = Modifier.weight(0.4f),
      )
      paneScope.Pane(
        key = "detail",
        item = detailItem,
        modifier = Modifier.weight(0.6f),
      )
    }
  }
}

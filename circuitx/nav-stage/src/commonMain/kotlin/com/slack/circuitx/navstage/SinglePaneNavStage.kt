// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Returns the default stage that renders only the active item in a single pane. Used as the
 * fallback when no strategy matches.
 */
@ExperimentalNavStageApi
@Suppress("UNCHECKED_CAST")
public fun <T : NavArgument> SinglePaneNavStage(): NavStage<T> =
  SinglePaneNavStageImpl as NavStage<T>

@OptIn(ExperimentalNavStageApi::class)
private object SinglePaneNavStageImpl : NavStage<NavArgument> {
  // Qualified so a third-party stage cannot collide and silently suppress stage transitions.
  override val key: Any = "com.slack.circuitx.navstage.single-pane"

  override fun visibleItems(args: NavStackList<NavArgument>): List<NavArgument> =
    listOf(args.active)

  @Composable
  override fun Content(
    items: List<NavArgument>,
    paneScope: NavStagePaneScope<NavArgument>,
    modifier: Modifier,
  ) {
    Box(modifier) { paneScope.Pane(key = PANE_KEY, item = items.single()) }
  }

  private const val PANE_KEY = "com.slack.circuitx.navstage.single-pane.pane"
}

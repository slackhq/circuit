// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Default stage that renders only the active item in a single pane. Used as the fallback when no
 * strategy matches.
 */
@ExperimentalNavStageApi
public class SinglePaneNavStage<T : NavArgument> private constructor() : NavStage<T> {
  override val key: Any = STAGE_KEY

  override fun visibleItems(args: NavStackList<T>): List<T> = listOf(args.active)

  @Composable
  override fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    Box(modifier) { paneScope.Pane(key = PANE_KEY, item = args.active) }
  }

  public companion object {
    // Qualified so a third-party stage cannot collide and silently suppress stage transitions.
    private const val STAGE_KEY = "com.slack.circuitx.navstage.single-pane"
    private const val PANE_KEY = "com.slack.circuitx.navstage.single-pane.pane"

    private val SinglePaneNavStageInstance = SinglePaneNavStage<NavArgument>()

    @Suppress("UNCHECKED_CAST")
    public fun <T : NavArgument> get(): SinglePaneNavStage<T> =
      SinglePaneNavStageInstance as SinglePaneNavStage<T>
  }
}

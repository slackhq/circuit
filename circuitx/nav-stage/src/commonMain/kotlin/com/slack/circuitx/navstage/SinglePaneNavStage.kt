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
  override val key: Any = "single-pane"

  @Composable
  override fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier) {
    Box(modifier) { paneScope.Pane(key = "single", item = args.active) }
  }

  public companion object {
    private val SinglePaneNavStageInstance = SinglePaneNavStage<NavArgument>()

    @Suppress("UNCHECKED_CAST")
    public fun <T : NavArgument> get(): SinglePaneNavStage<T> =
      SinglePaneNavStageInstance as SinglePaneNavStage<T>
  }
}

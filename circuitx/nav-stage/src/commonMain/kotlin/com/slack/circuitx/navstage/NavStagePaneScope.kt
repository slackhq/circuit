// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument

/**
 * Scope provided to [NavStage.Content] for placing navigation items into panes.
 *
 * Each [Pane] call renders a single navigation item with its own independent [PaneTransition]
 * animation when the item within that pane changes.
 */
@Stable
@ExperimentalNavStageApi
public interface NavStagePaneScope<T : NavArgument> {
  @Composable
  public fun Pane(
    key: Any,
    item: T,
    modifier: Modifier = Modifier,
    transition: PaneTransition = PaneTransition.Default,
  )
}

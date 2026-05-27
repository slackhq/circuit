// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument

@OptIn(ExperimentalNavStageApi::class)
internal class NavStagePaneScopeImpl<T : NavArgument>(
  private val content: @Composable (T) -> Unit,
  private val navEvent: PaneNavEvent,
) : NavStagePaneScope<T> {

  @Composable
  override fun Pane(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    transition.AnimatedPaneContent(
      targetItem = item,
      paneKey = key,
      navEvent = navEvent,
      modifier = modifier,
    ) { targetItem ->
      Box(Modifier) { content(targetItem) }
    }
  }
}

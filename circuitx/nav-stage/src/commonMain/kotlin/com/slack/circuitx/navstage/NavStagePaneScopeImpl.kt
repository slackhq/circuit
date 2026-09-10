// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.runtime.navigation.NavArgument

@OptIn(ExperimentalNavStageApi::class)
internal class NavStagePaneScopeImpl<T : NavArgument>(
  private val content: @Composable (T) -> Unit,
  private val navEvent: AnimatedNavEvent,
) : NavStagePaneScope<T> {

  @Composable
  override fun Pane(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    transition.AnimatedPaneContent(
      targetItem = item,
      paneKey = key,
      navEvent = navEvent,
      modifier = modifier,
    ) { targetItem ->
      // A pane is on screen, so its record is active. Without this the host derives activeness from
      // "is the current record", which is false for every pane but one, freezing those presenters.
      ProvideRecordLifecycle(isActive = true) { content(targetItem) }
    }
  }
}

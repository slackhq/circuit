// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument

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

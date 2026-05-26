// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

@Stable
@ExperimentalNavStageApi
public interface NavStage<T : NavArgument> {
  public val key: Any

  @Composable
  public fun Content(
    args: NavStackList<T>,
    paneScope: NavStagePaneScope<T>,
    modifier: Modifier,
  )
}

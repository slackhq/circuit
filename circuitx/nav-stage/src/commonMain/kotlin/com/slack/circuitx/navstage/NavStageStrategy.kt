// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Determines which [NavStage] to use for a given navigation stack state.
 *
 * Returns `null` to fall back to [SinglePaneNavStage]. Implementations typically inspect window size
 * class and stack contents to decide between single-pane and multi-pane layouts.
 */
@Stable
@ExperimentalNavStageApi
public interface NavStageStrategy {
  @Composable public fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>?
}

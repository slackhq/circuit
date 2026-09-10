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
 * Returns `null` to decline, in which case the next strategy is consulted and [SinglePaneNavStage] is
 * the final fallback. Implementations typically inspect window size class and stack contents to
 * decide between single-pane and multi-pane layouts.
 *
 * Every strategy in a [NavStageDecoration] is invoked on each pass and the first non-null result
 * wins, so keep [calculateStage] cheap and free of side effects. It may also be called more than
 * once per frame, for the current stack and for a stack a transition is animating from.
 */
@Stable
@ExperimentalNavStageApi
public interface NavStageStrategy {
  @Composable public fun <T : NavArgument> calculateStage(args: NavStackList<T>): NavStage<T>?
}

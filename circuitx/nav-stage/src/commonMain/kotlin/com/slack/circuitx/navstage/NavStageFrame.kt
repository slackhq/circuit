// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Wraps the entire stage content with a decorative frame (e.g. borders, elevation, clipping).
 *
 * The frame receives the resolved [NavStage] and full [NavStackList] so it can adapt its decoration
 * based on the active layout. [None] applies no decoration beyond a simple [Box].
 */
@Stable
@ExperimentalNavStageApi
public interface NavStageFrame {
  @Composable
  public fun <T : NavArgument> Content(
    modifier: Modifier,
    stage: NavStage<T>,
    args: NavStackList<T>,
    stageContent: @Composable () -> Unit,
  )

  public companion object {
    public val None: NavStageFrame =
      object : NavStageFrame {
        @Composable
        override fun <T : NavArgument> Content(
          modifier: Modifier,
          stage: NavStage<T>,
          args: NavStackList<T>,
          stageContent: @Composable () -> Unit,
        ) {
          Box(modifier) { stageContent() }
        }
      }
  }
}

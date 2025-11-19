// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen

/** Presentation logic for currently visible routes of a navigable UI. */
@Stable
public interface NavDecoration {
  @Composable
  public fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  )
}

/** Argument provided to [NavDecoration] that exposes the underlying [Screen]. */
public interface NavArgument {
  public val screen: Screen
  public val key: String
}

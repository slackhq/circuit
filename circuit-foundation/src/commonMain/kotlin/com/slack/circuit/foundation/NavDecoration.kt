// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/** Presentation logic for currently visible routes of a navigable UI. */
@Stable
public interface NavDecoration {

  /**
   * Renders decorated content for the current navigation state.
   *
   * @param T The type of navigation argument that extends NavArgument
   * @param args The current navigation stack.
   * @param navigator The current navigator instance.
   * @param modifier The modifier to be applied to the decorated content.
   * @param content The content for a given navigation argument.
   */
  @Composable
  public fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  )
}

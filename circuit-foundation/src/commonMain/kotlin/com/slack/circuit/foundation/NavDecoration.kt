// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Presentation logic for currently visible routes of a navigable UI.
 *
 * A decoration decides which records to show and where. It can show one record, like
 * [AnimatedNavDecoration][com.slack.circuit.foundation.animation.AnimatedNavDecoration], or several
 * at once, like a list beside its detail. These rules apply to every implementation:
 * - A decoration may call `content` from any place in its layout. The content of a record is
 *   movable, so a record keeps its state when the call for it moves to another place.
 * - A decoration must call `content` at most once per record in a composition. Two calls for one
 *   record share the same saved and retained state keys.
 * - Only the current record is active by default. A decoration that shows other records must set
 *   their lifecycle with [ProvideRecordLifecycle] around `content`, or they stay paused.
 * - A decoration may wrap its arguments in another [NavArgument] type, or add arguments that have
 *   no record, before it passes them to another decoration. It must then map each one back to the
 *   original argument when it calls `content`.
 */
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

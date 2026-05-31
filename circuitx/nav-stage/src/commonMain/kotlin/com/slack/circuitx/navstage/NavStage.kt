// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * A layout strategy that renders one or more panes from the navigation stack.
 *
 * Stages are stable across navigation events within the same layout type (identified by [key]).
 * They read items from [args] in [Content] rather than capturing them at construction, so the stage
 * instance can be reused as the stack changes without recomposition of the layout structure.
 */
@Stable
@ExperimentalNavStageApi
public interface NavStage<T : NavArgument> {
  /**
   * Stable identity for this stage layout. Transitions animate between stages with different keys.
   */
  public val key: Any

  /**
   * Returns the keys of navigation items this stage will render for the given [args].
   *
   * Used during stage transitions to detect items shared between the outgoing and incoming stages.
   * Shared items are rendered as placeholders (with animated bounds) in the outgoing stage to avoid
   * composing a `movableContentOf` in multiple places simultaneously.
   *
   * Override this when the stage renders additional items beyond the active one (e.g. a list pane).
   */
  public fun renderedItemKeys(args: NavStackList<T>): Set<Any> = setOf(args.active.key)

  /**
   * Renders the stage layout, using [paneScope] to place individual navigation items into panes.
   */
  @Composable
  public fun Content(args: NavStackList<T>, paneScope: NavStagePaneScope<T>, modifier: Modifier)
}

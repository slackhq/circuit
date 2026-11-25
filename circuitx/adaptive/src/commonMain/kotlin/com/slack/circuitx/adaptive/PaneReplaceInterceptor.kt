// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed

/**
 * Replaces the open detail when a screen that [replaces] it opens while the window shows panes.
 * Picking a second list item then swaps the detail, and back returns to the empty end pane in one
 * step.
 *
 * On each call, the interceptor asks the strategy of the [PaneNavDecoration] for the scene of the
 * current stack. It does not wait for a composition, so two calls in one frame both see the right
 * start pane. The window is the one from the last composition.
 *
 * The rule looks only at the records above the start pane's record:
 * - A screen that replaces none of them is pushed as usual.
 * - A screen that replaces one of them takes the place of the lowest such record and every record
 *   above it.
 * - A screen that is equal to that record pops back to it.
 *
 * By default a screen replaces an open screen of the same class. Pass [replaces] for other rules. A
 * list with two detail types can replace any detail with any other:
 * ```kotlin
 * PaneReplaceInterceptor(host) { open, incoming -> open is ListDetail && incoming is ListDetail }
 * ```
 *
 * A detail that pushes more screens of its own class can return false to stack them.
 *
 * The pops and the push go out as one [InterceptedResult.RewriteAll], so the decoration never
 * renders the stack between them.
 *
 * @param host The [PaneHost] of the [PaneNavDecoration] that renders the intercepted navigator.
 * @param replaces Whether `incoming`, the screen that opens, takes the place of `open`, a screen
 *   that is open beside the start pane.
 */
@ExperimentalCircuitAdaptiveApi
public class PaneReplaceInterceptor(
  private val host: PaneHost,
  private val replaces: (open: Screen, incoming: Screen) -> Boolean = { open, incoming ->
    open::class == incoming::class
  },
) : NavigationInterceptor {

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    // Top first, root last.
    val stack = navigationContext.peekBackStack() ?: return Skipped
    // A scene whose start pane has the full width still counts: the next screen opens beside it.
    val endPane = stack.take(recordsAbovePrimary(stack) ?: return Skipped)
    val match = endPane.indexOfLast { open -> replaces(open, screen) }
    return when {
      // Also the path for the push that this interceptor rewrote, which no longer has a match.
      match == -1 -> Skipped
      endPane[match] == screen -> {
        if (match == 0) {
          SuccessConsumed
        } else {
          InterceptedResult.RewriteAll(List(match) { NavEvent.Pop() })
        }
      }
      else -> {
        InterceptedResult.RewriteAll(List(match + 1) { NavEvent.Pop() } + NavEvent.GoTo(screen))
      }
    }
  }

  /**
   * Returns how many records of [stack] (top first) are above the start pane's record, or null when
   * the strategy shows one record for this stack or no decoration has composed yet.
   */
  private fun recordsAbovePrimary(stack: List<Screen>): Int? {
    val strategy = host.strategy ?: return null
    val window = host.window ?: return null
    if (stack.isEmpty()) {
      return null
    }
    // The strategy names the start pane's record by key. These records have no keys here, so
    // each gets its distance from the root.
    val args = stack.mapIndexed { index, screen -> StackArg(screen, stack.size - 1 - index) }
    val scene =
      strategy.scene(window, navStackListOf(activeItem = args[0], backwardItems = args.drop(1)))
        ?: return null
    return args.indexOfFirst { it.key == scene.primaryKey }.takeIf { it >= 0 }
  }

  /** A record of the intercepted stack, for a strategy call. */
  private class StackArg(override val screen: Screen, fromRoot: Int) : NavArgument {
    override val key: String = "circuitx-adaptive-stack-$fromRoot"
  }
}

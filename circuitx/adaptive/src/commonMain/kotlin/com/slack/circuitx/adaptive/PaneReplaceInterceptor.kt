// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed

/**
 * Replaces the open detail when a screen that [replaces] it opens while [host] has a scene. Picking
 * a second list item then swaps the detail, and back returns to the empty right pane in one step.
 *
 * The rule looks only at the records above the left pane's record:
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
 *   that is open beside the left pane.
 */
@ExperimentalCircuitAdaptiveApi
public class PaneReplaceInterceptor(
  private val host: PaneHost,
  private val replaces: (open: Screen, incoming: Screen) -> Boolean = { open, incoming ->
    open::class == incoming::class
  },
) : NavigationInterceptor {

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    // A scene whose left pane has the full width still counts: the next screen opens beside it.
    if (host.scene == null) {
      return Skipped
    }
    // Top first, root last.
    val stack = navigationContext.peekBackStack() ?: return Skipped
    val rightPane = stack.dropLast(host.primaryDepth)
    val match = rightPane.indexOfLast { open -> replaces(open, screen) }
    return when {
      // Also the path for the push that this interceptor rewrote, which no longer has a match.
      match == -1 -> Skipped
      rightPane[match] == screen -> {
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
}

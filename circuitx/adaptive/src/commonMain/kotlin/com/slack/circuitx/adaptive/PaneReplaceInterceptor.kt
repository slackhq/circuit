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
 * Replaces the open detail when another screen of the same type opens while [host] shows two panes.
 * Picking a second list item then swaps the detail, and back returns to the empty right pane in one
 * step.
 *
 * The rule looks only at the records in the right pane:
 * - A screen of a type that is not there is pushed as usual.
 * - A screen of a type that is there replaces that record and every record above it.
 * - A screen that is equal to a record there pops back to that record.
 *
 * The pops and the push go out as one [InterceptedResult.RewriteAll], so the decoration never
 * renders the stack between them.
 *
 * @param host The [PaneHost] of the [PaneNavDecoration] that renders the intercepted navigator.
 */
@ExperimentalCircuitAdaptiveApi
public class PaneReplaceInterceptor(private val host: PaneHost) : NavigationInterceptor {

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    if (!host.isSplit) {
      return Skipped
    }
    // Top first, root last.
    val stack = navigationContext.peekBackStack() ?: return Skipped
    val rightPane = stack.dropLast(host.primaryDepth)
    val match = rightPane.indexOfLast { it::class == screen::class }
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

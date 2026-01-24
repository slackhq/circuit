// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped

/**
 * Navigation interceptor for slide-over navigation that transforms navigation events at boundaries.
 *
 * This interceptor handles two cases:
 * 1. `pop()` at root converts to `backward()` so we can return forward to the current screen.
 * 2. `goTo(screen)` from root where screen matches the next and top of the forward stack ->
 *    converts to `forward()` This allows navigating to a screen that's already in the forward
 *    history to use the forward navigation instead of creating a new entry.
 */
class SlideOverNavigationInterceptor : NavigationInterceptor {

  override fun pop(result: PopResult?, navigationContext: NavigationContext): InterceptedResult {
    val navStack = navigationContext.peekNavStack() ?: return Skipped
    // We would pop back to the root, just go backward.
    val isNextRoot = navStack.backwardItems.singleOrNull() == navStack.root
    return if (isNextRoot) {
      InterceptedResult.Rewrite(NavEvent.Backward)
    } else {
      Skipped
    }
  }

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    val navStack = navigationContext.peekNavStack() ?: return Skipped
    val isAtRoot = navStack.root == navStack.active
    val nextForward = navStack.forwardItems.firstOrNull()
    // At root and the next screen forward is the same screen, just go forward to it.
    return if (isAtRoot && nextForward == screen) {
      InterceptedResult.Rewrite(NavEvent.Forward)
    } else {
      Skipped
    }
  }
}

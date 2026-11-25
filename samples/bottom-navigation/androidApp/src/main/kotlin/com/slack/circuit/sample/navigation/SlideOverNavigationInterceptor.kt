// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped

typealias ConfirmRewritePop = (navStack: NavStackList<Screen>, result: PopResult?) -> Boolean

typealias ConfirmRewriteGoTo = (navStack: NavStackList<Screen>, screen: Screen) -> Boolean

val ConfirmRewritePopAlways: ConfirmRewritePop = { _, _ -> true }
val ConfirmRewritePopAtRoot: ConfirmRewritePop = { navStack, _ ->
  navStack.backwardItems.singleOrNull() == navStack.root
}

val ConfirmRewriteGoToSame: ConfirmRewriteGoTo = { navStack, screen ->
  navStack.forwardItems.firstOrNull() == screen
}

/**
 * Navigation interceptor for slide-over navigation that transforms navigation events at boundaries.
 */
data class SlideOverNavigationInterceptor(
  val confirmRewritePop: ConfirmRewritePop = ConfirmRewritePopAlways,
  val confirmRewriteGoTo: ConfirmRewriteGoTo = ConfirmRewriteGoToSame,
) : NavigationInterceptor {

  override fun pop(result: PopResult?, navigationContext: NavigationContext): InterceptedResult {
    val navStack = navigationContext.peekNavStack() ?: return Skipped
    return if (confirmRewritePop(navStack, result)) {
      InterceptedResult.Rewrite(NavEvent.Backward)
    } else {
      Skipped
    }
  }

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    val navStack = navigationContext.peekNavStack() ?: return Skipped
    return if (confirmRewriteGoTo(navStack, screen)) {
      InterceptedResult.Rewrite(NavEvent.Forward)
    } else {
      Skipped
    }
  }
}

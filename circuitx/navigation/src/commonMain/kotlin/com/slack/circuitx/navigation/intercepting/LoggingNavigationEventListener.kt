// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/** A [NavigationEventListener] that adds logging to Circuit navigation. */
public class LoggingNavigationEventListener(private val logger: NavigationLogger) :
  NavigationEventListener {

  override fun onBackStackChanged(backStack: List<Screen>, navigationContext: NavigationContext) {
    logger.log("Backstack changed ${backStack.joinToString { it.loggingName() ?: "" }}")
  }

  override fun goTo(screen: Screen, navigationContext: NavigationContext) {
    logger.log("goTo ${screen.loggingName()}")
  }

  override fun pop(result: PopResult?, navigationContext: NavigationContext) {
    // Logs the screen that was popped.
    logger.log("pop ${navigationContext.peekBackStack()?.firstOrNull()?.loggingName() ?: ""}")
  }
}

private fun Screen.loggingName() = this::class.simpleName

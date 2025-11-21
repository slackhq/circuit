// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.NavStackList
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/** A [NavigationEventListener] that adds logging to Circuit navigation. */
public class LoggingNavigationEventListener(private val logger: NavigationLogger) :
  NavigationEventListener {

  override fun onBackStackChanged(backStack: List<Screen>, navigationContext: NavigationContext) {
    logger.log("Backstack changed ${backStack.joinToString { it.loggingName() ?: "" }}")
  }

  override fun onNavStackChanged(
    navStack: NavStackList<Screen>?,
    navigationContext: NavigationContext,
  ) {
    val forwardStack = navStack?.forward?.reversed()?.joinToString { it.loggingName() ?: "" } ?: ""
    val backwardStack = navStack?.backward?.joinToString { it.loggingName() ?: "" } ?: ""
    val currentScreen = navStack?.current?.loggingName() ?: ""
    logger.log("NavStack changed [$forwardStack] $currentScreen [$backwardStack]")
  }

  override fun goTo(screen: Screen, navigationContext: NavigationContext) {
    logger.log("goTo ${screen.loggingName()}")
  }

  override fun pop(result: PopResult?, navigationContext: NavigationContext) {
    // Logs the screen that was popped.
    logger.log("pop ${navigationContext.peekBackStack()?.firstOrNull()?.loggingName() ?: ""}")
  }

  override fun forward(navigationContext: NavigationContext) {
    val screen = navigationContext.peekNavStack()?.forward?.firstOrNull()?.loggingName() ?: ""
    // Logs the screen that was navigated to.
    logger.log("forward $screen")
  }

  override fun backward(navigationContext: NavigationContext) {
    val screen = navigationContext.peekNavStack()?.backward?.firstOrNull()?.loggingName() ?: ""
    // Logs the screen that was navigated to.
    logger.log("backward $screen")
  }

  override fun resetRoot(
    newRoot: Screen,
    options: Navigator.StateOptions,
    navigationContext: NavigationContext,
  ) {
    logger.log("resetRoot ${newRoot.loggingName()} with options $options")
  }
}

private fun Screen.loggingName() = this::class.simpleName

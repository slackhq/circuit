// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/** A [NavigationEventListener] that adds logging to Circuit navigation. */
public class LoggingNavigationEventListener(private val logger: NavigationLogger) :
  NavigationEventListener {

  override fun onBackStackChanged(backStack: ImmutableList<Screen>) {
    logger.log("Backstack changed ${backStack.joinToString { it.loggingName() ?: "" }}")
  }

  override fun goTo(screen: Screen) {
    logger.log("goTo ${screen.loggingName()}")
  }

  override fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {
    logger.log("pop ${backStack.firstOrNull()?.loggingName()}")
  }
}

private fun Screen.loggingName() = this::class.simpleName

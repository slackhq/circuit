// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import android.util.Log
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/** A [CircuitNavigationEventListener] that adds Logcat logging for Circuit navigation. */
public object LoggingNavigationEventListener : CircuitNavigationEventListener {

  override fun onBackStackChanged(backStack: ImmutableList<Screen>) {
    log("new backstack ${backStack.joinToString { it.loggingName() ?: "" }}")
  }

  override fun goTo(screen: Screen) {
    log("goTo ${screen.loggingName()}")
  }

  override fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {
    log("pop ${backStack.firstOrNull()?.loggingName()}")
  }

  private fun log(message: String) = Log.i("Circuit Navigation", message)
}

private fun Screen.loggingName() = this::class.simpleName

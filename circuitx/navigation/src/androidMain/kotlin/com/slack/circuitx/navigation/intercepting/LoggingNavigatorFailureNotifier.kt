// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import android.util.Log

/**
 * A [InterceptingNavigator.FailureNotifier] that adds Logcat logging for CircuitX navigation
 * interception failures.
 */
public object LoggingNavigatorFailureNotifier : InterceptingNavigator.FailureNotifier {
  override fun goToFailure(InterceptedResult: InterceptedResult.Failure) {
    log("goToFailure: $InterceptedResult")
  }

  override fun popFailure(InterceptedResult: InterceptedResult.Failure) {
    log("popFailure: $InterceptedResult")
  }

  override fun rootResetFailure(InterceptedResult: InterceptedResult.Failure) {
    log("rootResetFailure: $InterceptedResult")
  }

  private fun log(message: String) = Log.i("Circuit Navigation", message)
}

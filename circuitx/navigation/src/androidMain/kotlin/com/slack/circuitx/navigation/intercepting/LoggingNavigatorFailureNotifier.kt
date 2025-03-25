// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import android.util.Log

/**
 * A [CircuitInterceptingNavigator.FailureNotifier] that adds Logcat logging for CircuitX navigation
 * interception failures.
 */
public object LoggingNavigatorFailureNotifier : CircuitInterceptingNavigator.FailureNotifier {
  override fun goToInterceptorFailure(result: CircuitNavigationInterceptor.Result.Failure) {
    log("goToInterceptorFailure: $result")
  }

  override fun popInterceptorFailure(result: CircuitNavigationInterceptor.Result.Failure) {
    log("popInterceptorFailure: $result")
  }

  private fun log(message: String) = Log.i("Circuit Navigation", message)
}

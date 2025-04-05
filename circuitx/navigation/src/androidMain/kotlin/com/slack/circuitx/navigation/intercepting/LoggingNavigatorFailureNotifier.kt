// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import android.util.Log

/**
 * A [CircuitInterceptingNavigator.FailureNotifier] that adds Logcat logging for CircuitX navigation
 * interception failures.
 */
public object LoggingNavigatorFailureNotifier : CircuitInterceptingNavigator.FailureNotifier {
  override fun goToInterceptorFailure(interceptorResult: InterceptorResult.Failure) {
    log("goToInterceptorFailure: $interceptorResult")
  }

  override fun popInterceptorFailure(interceptorResult: InterceptorResult.Failure) {
    log("popInterceptorFailure: $interceptorResult")
  }

  override fun rootResetInterceptorFailure(interceptorResult: InterceptorResult.Failure) {
    log("rootResetInterceptorFailure: $interceptorResult")
  }

  private fun log(message: String) = Log.i("Circuit Navigation", message)
}

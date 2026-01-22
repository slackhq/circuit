// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

/**
 * A [InterceptingNavigator.FailureNotifier] that adds Logcat logging for CircuitX navigation
 * interception failures.
 */
public class LoggingNavigatorFailureNotifier(private val logger: NavigationLogger) :
  InterceptingNavigator.FailureNotifier {
  override fun goToFailure(interceptedResult: InterceptedResult.Failure) {
    logger.log("goToFailure: $interceptedResult")
  }

  override fun popFailure(interceptedResult: InterceptedResult.Failure) {
    logger.log("popFailure: $interceptedResult")
  }

  override fun rootResetFailure(interceptedResult: InterceptedResult.Failure) {
    logger.log("rootResetFailure: $interceptedResult")
  }

  override fun forwardFailure(interceptedResult: InterceptedResult.Failure) {
    logger.log("forwardFailure: $interceptedResult")
  }

  override fun backwardFailure(interceptedResult: InterceptedResult.Failure) {
    logger.log("backwardFailure: $interceptedResult")
  }
}

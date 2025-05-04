// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine

class FakeFailureNotifier : InterceptingNavigator.FailureNotifier {

  private val goToFailures = Turbine<InterceptedResult.Failure>()
  private val popFailures = Turbine<InterceptedResult.Failure>()
  private val resetRootFailures = Turbine<InterceptedResult.Failure>()

  override fun goToFailure(interceptorResult: InterceptedResult.Failure) {
    goToFailures.add(interceptorResult)
  }

  override fun popFailure(interceptorResult: InterceptedResult.Failure) {
    popFailures.add(interceptorResult)
  }

  override fun rootResetFailure(interceptorResult: InterceptedResult.Failure) {
    resetRootFailures.add(interceptorResult)
  }

  suspend fun awaitGoToFailure(): InterceptedResult.Failure = goToFailures.awaitItem()

  suspend fun awaitPopFailure(): InterceptedResult.Failure = popFailures.awaitItem()

  suspend fun awaitRootResetFailure(): InterceptedResult.Failure = resetRootFailures.awaitItem()
}

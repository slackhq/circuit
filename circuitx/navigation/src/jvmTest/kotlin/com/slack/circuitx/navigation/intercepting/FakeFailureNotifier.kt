// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine

class FakeFailureNotifier : InterceptingNavigator.FailureNotifier {

  private val goToFailures = Turbine<InterceptedResult.Failure>()
  private val popFailures = Turbine<InterceptedResult.Failure>()
  private val resetRootFailures = Turbine<InterceptedResult.Failure>()

  override fun goToFailure(interceptedResult: InterceptedResult.Failure) {
    goToFailures.add(interceptedResult)
  }

  override fun popFailure(interceptedResult: InterceptedResult.Failure) {
    popFailures.add(interceptedResult)
  }

  override fun rootResetFailure(interceptedResult: InterceptedResult.Failure) {
    resetRootFailures.add(interceptedResult)
  }

  suspend fun awaitGoToFailure(): InterceptedResult.Failure = goToFailures.awaitItem()

  suspend fun awaitPopFailure(): InterceptedResult.Failure = popFailures.awaitItem()

  suspend fun awaitRootResetFailure(): InterceptedResult.Failure = resetRootFailures.awaitItem()
}

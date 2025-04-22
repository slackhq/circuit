package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine

class FakeFailureNotifier : CircuitInterceptingNavigator.FailureNotifier {

  private val goToFailures = Turbine<InterceptorResult.Failure>()
  private val popFailures = Turbine<InterceptorResult.Failure>()
  private val resetRootFailures = Turbine<InterceptorResult.Failure>()

  override fun goToInterceptorFailure(interceptorResult: InterceptorResult.Failure) {
    goToFailures.add(interceptorResult)
  }

  override fun popInterceptorFailure(interceptorResult: InterceptorResult.Failure) {
    popFailures.add(interceptorResult)
  }

  override fun rootResetInterceptorFailure(interceptorResult: InterceptorResult.Failure) {
    resetRootFailures.add(interceptorResult)
  }

  suspend fun awaitGoToFailure(): InterceptorResult.Failure = goToFailures.awaitItem()

  suspend fun awaitPopFailure(): InterceptorResult.Failure = popFailures.awaitItem()

  suspend fun awaitRootResetFailure(): InterceptorResult.Failure = resetRootFailures.awaitItem()
}

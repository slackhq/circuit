// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/** A fake implementation of NavigationInterceptor for testing. */
class FakeNavigationInterceptor : NavigationInterceptor {

  private val goToEvents = Turbine<GoToEvent>()
  private val popEvents = Turbine<PopEvent>()
  private val resetRootEvents = Turbine<ResetRootEvent>()
  private val forwardEvents = Turbine<ForwardEvent>()
  private val backwardEvents = Turbine<BackwardEvent>()

  private val goToResults = mutableListOf<InterceptedGoToResult>()
  private val popResults = mutableListOf<InterceptedPopResult>()
  private val resetRootResults = mutableListOf<InterceptedResetRootResult>()
  private val forwardResults = mutableListOf<InterceptedResult>()
  private val backwardResults = mutableListOf<InterceptedResult>()

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedGoToResult {
    val result = goToResults.removeFirst()
    goToEvents.add(GoToEvent(navigationContext.peekNavStack(), screen, result))
    return result
  }

  override fun pop(result: PopResult?, navigationContext: NavigationContext): InterceptedPopResult {
    val interceptorPopResult = popResults.removeFirst()
    popEvents.add(PopEvent(navigationContext.peekNavStack(), result, interceptorPopResult))
    return interceptorPopResult
  }

  override fun resetRoot(
    newRoot: Screen,
    options: StateOptions,
    navigationContext: NavigationContext,
  ): InterceptedResetRootResult {
    val interceptorResetRootResult = resetRootResults.removeFirst()
    resetRootEvents.add(
      ResetRootEvent(navigationContext.peekNavStack(), newRoot, interceptorResetRootResult, options)
    )
    return interceptorResetRootResult
  }

  override fun forward(navigationContext: NavigationContext): InterceptedResult {
    val result = forwardResults.removeFirst()
    forwardEvents.add(ForwardEvent(navigationContext.peekNavStack(), result))
    return result
  }

  override fun backward(navigationContext: NavigationContext): InterceptedResult {
    val result = backwardResults.removeFirst()
    backwardEvents.add(BackwardEvent(navigationContext.peekNavStack(), result))
    return result
  }

  fun queueGoTo(vararg interceptorGoToResult: InterceptedGoToResult) {
    goToResults.addAll(interceptorGoToResult)
  }

  fun queuePop(vararg interceptorPopResult: InterceptedPopResult) {
    popResults.addAll(interceptorPopResult)
  }

  fun queueResetRoot(vararg interceptorResetRootResult: InterceptedResetRootResult) {
    resetRootResults.addAll(interceptorResetRootResult)
  }

  fun queueForward(vararg interceptorResult: InterceptedResult) {
    forwardResults.addAll(interceptorResult)
  }

  fun queueBackward(vararg interceptorResult: InterceptedResult) {
    backwardResults.addAll(interceptorResult)
  }

  /** Awaits the next [goTo] or throws if no goTo are performed. */
  suspend fun awaitGoTo(): GoToEvent = goToEvents.awaitItem()

  /** Awaits the next [pop] event or throws if no pops are performed. */
  suspend fun awaitPop(): PopEvent = popEvents.awaitItem()

  /** Awaits the next [resetRoot] or throws if no resets were performed. */
  suspend fun awaitResetRoot(): ResetRootEvent = resetRootEvents.awaitItem()

  /** Awaits the next [forward] event or throws if no forwards are performed. */
  suspend fun awaitForward(): ForwardEvent = forwardEvents.awaitItem()

  /** Awaits the next [backward] event or throws if no backwards are performed. */
  suspend fun awaitBackward(): BackwardEvent = backwardEvents.awaitItem()

  fun assertGoToIsEmpty() {
    goToEvents.ensureAllEventsConsumed()
  }

  fun assertPopIsEmpty() {
    popEvents.ensureAllEventsConsumed()
  }

  fun assertResetRootIsEmpty() {
    resetRootEvents.ensureAllEventsConsumed()
  }

  fun assertForwardIsEmpty() {
    forwardEvents.ensureAllEventsConsumed()
  }

  fun assertBackwardIsEmpty() {
    backwardEvents.ensureAllEventsConsumed()
  }

  /** Represents a recorded [NavigationInterceptor.goTo] event. */
  data class GoToEvent(
    val navStack: NavStackList<Screen>?,
    val screen: Screen,
    val interceptorGoToResult: InterceptedGoToResult,
  )

  /** Represents a recorded [NavigationInterceptor.pop] event. */
  data class PopEvent(
    val navStack: NavStackList<Screen>?,
    val result: PopResult?,
    val interceptorGoToResult: InterceptedPopResult,
  )

  /** Represents a recorded [NavigationInterceptor.resetRoot] event. */
  data class ResetRootEvent(
    val navStack: NavStackList<Screen>?,
    val newRoot: Screen,
    val interceptorResetRootResult: InterceptedResetRootResult,
    val options: StateOptions = StateOptions.Default,
  )

  /** Represents a recorded [NavigationInterceptor.forward] event. */
  data class ForwardEvent(
    val navStack: NavStackList<Screen>?,
    val interceptorResult: InterceptedResult,
  )

  /** Represents a recorded [NavigationInterceptor.backward] event. */
  data class BackwardEvent(
    val navStack: NavStackList<Screen>?,
    val interceptorResult: InterceptedResult,
  )
}

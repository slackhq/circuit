// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/** A fake implementation of NavigationInterceptor for testing. */
class FakeNavigationInterceptor : NavigationInterceptor {

  private val goToEvents = Turbine<GoToEvent>()
  private val popEvents = Turbine<PopEvent>()
  private val resetRootEvents = Turbine<ResetRootEvent>()

  private val goToResults = mutableListOf<InterceptedGoToResult>()
  private val popResults = mutableListOf<InterceptedPopResult>()
  private val resetRootResults = mutableListOf<InterceptedResetRootResult>()

  override fun goTo(screen: Screen): InterceptedGoToResult {
    val result = goToResults.removeFirst()
    goToEvents.add(GoToEvent(screen, result))
    return result
  }

  override fun pop(peekBackStack: ImmutableList<Screen>, result: PopResult?): InterceptedPopResult {
    val interceptorPopResult = popResults.removeFirst()
    popEvents.add(PopEvent(peekBackStack, result, interceptorPopResult))
    return interceptorPopResult
  }

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): InterceptedResetRootResult {
    val interceptorResetRootResult = resetRootResults.removeFirst()
    resetRootEvents.add(
      ResetRootEvent(newRoot, interceptorResetRootResult, saveState, restoreState)
    )
    return interceptorResetRootResult
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

  /** Awaits the next [goTo] or throws if no goTo are performed. */
  suspend fun awaitGoTo(): GoToEvent = goToEvents.awaitItem()

  /** Awaits the next [pop] event or throws if no pops are performed. */
  suspend fun awaitPop(): PopEvent = popEvents.awaitItem()

  /** Awaits the next [resetRoot] or throws if no resets were performed. */
  suspend fun awaitResetRoot(): ResetRootEvent = resetRootEvents.awaitItem()

  fun assertGoToIsEmpty() {
    goToEvents.ensureAllEventsConsumed()
  }

  fun assertPopIsEmpty() {
    popEvents.ensureAllEventsConsumed()
  }

  fun assertResetRootIsEmpty() {
    resetRootEvents.ensureAllEventsConsumed()
  }

  /** Represents a recorded [NavigationInterceptor.goTo] event. */
  data class GoToEvent(val screen: Screen, val interceptorGoToResult: InterceptedGoToResult)

  /** Represents a recorded [NavigationInterceptor.pop] event. */
  data class PopEvent(
    val peekBackStack: ImmutableList<Screen>,
    val result: PopResult?,
    val interceptorGoToResult: InterceptedPopResult,
  )

  /** Represents a recorded [NavigationInterceptor.resetRoot] event. */
  data class ResetRootEvent(
    val newRoot: Screen,
    val interceptorResetRootResult: InterceptedResetRootResult,
    val saveState: Boolean = false,
    val restoreState: Boolean = false,
  )
}

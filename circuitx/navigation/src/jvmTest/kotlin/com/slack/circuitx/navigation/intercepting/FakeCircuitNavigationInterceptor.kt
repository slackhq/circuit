package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine
import com.slack.circuit.runtime.resetRoot
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/** A fake implementation of CircuitNavigationInterceptor for testing. */
class FakeCircuitNavigationInterceptor : CircuitNavigationInterceptor {

  private val goToEvents = Turbine<GoToEvent>()
  private val popEvents = Turbine<PopEvent>()
  private val resetRootEvents = Turbine<ResetRootEvent>()

  private val goToResults = mutableListOf<InterceptorGoToResult>()
  private val popResults = mutableListOf<InterceptorPopResult>()
  private val resetRootResults = mutableListOf<InterceptorResetRootResult>()

  override fun goTo(screen: Screen): InterceptorGoToResult {
    val result = goToResults.removeFirst()
    goToEvents.add(GoToEvent(screen, result))
    return result
  }

  override fun pop(peekBackStack: ImmutableList<Screen>, result: PopResult?): InterceptorPopResult {
    val interceptorPopResult = popResults.removeFirst()
    popEvents.add(PopEvent(peekBackStack, result, interceptorPopResult))
    return interceptorPopResult
  }

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): InterceptorResetRootResult {
    val interceptorResetRootResult = resetRootResults.removeFirst()
    resetRootEvents.add(
      ResetRootEvent(newRoot, interceptorResetRootResult, saveState, restoreState)
    )
    return interceptorResetRootResult
  }

  fun queueGoTo(vararg interceptorGoToResult: InterceptorGoToResult) {
    goToResults.addAll(interceptorGoToResult)
  }

  fun queuePop(vararg interceptorPopResult: InterceptorPopResult) {
    popResults.addAll(interceptorPopResult)
  }

  fun queueResetRoot(vararg interceptorResetRootResult: InterceptorResetRootResult) {
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

  /** Represents a recorded [CircuitNavigationInterceptor.goTo] event. */
  data class GoToEvent(val screen: Screen, val interceptorGoToResult: InterceptorGoToResult)

  /** Represents a recorded [CircuitNavigationInterceptor.pop] event. */
  data class PopEvent(
    val peekBackStack: ImmutableList<Screen>,
    val result: PopResult?,
    val interceptorGoToResult: InterceptorPopResult,
  )

  /** Represents a recorded [CircuitNavigationInterceptor.resetRoot] event. */
  data class ResetRootEvent(
    val newRoot: Screen,
    val interceptorResetRootResult: InterceptorResetRootResult,
    val saveState: Boolean = false,
    val restoreState: Boolean = false,
  )
}

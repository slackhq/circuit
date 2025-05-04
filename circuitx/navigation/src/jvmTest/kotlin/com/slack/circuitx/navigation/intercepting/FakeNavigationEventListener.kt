package com.slack.circuitx.navigation.intercepting

import app.cash.turbine.Turbine
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

class FakeNavigationEventListener : NavigationEventListener {

  private val onBackStackChangedEvents = Turbine<OnBackStackChangedEvent>()
  private val goToEvents = Turbine<GoToEvent>()
  private val popEvents = Turbine<PopEvent>()
  private val resetRootEvents = Turbine<ResetRootEvent>()

  override fun onBackStackChanged(backStack: ImmutableList<Screen>) {
    onBackStackChangedEvents.add(OnBackStackChangedEvent(backStack))
  }

  override fun goTo(screen: Screen) {
    goToEvents.add(GoToEvent(screen))
  }

  override fun pop(backStack: ImmutableList<Screen>, result: PopResult?) {
    popEvents.add(PopEvent(backStack, result))
  }

  override fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean) {
    resetRootEvents.add(ResetRootEvent(newRoot, saveState, restoreState))
  }

  /** Awaits the next [goTo] or throws if no goTo are performed. */
  suspend fun awaitGoTo(): GoToEvent = goToEvents.awaitItem()

  /** Awaits the next [pop] event or throws if no pops are performed. */
  suspend fun awaitPop(): PopEvent = popEvents.awaitItem()

  /** Awaits the next [resetRoot] or throws if no resets were performed. */
  suspend fun awaitResetRoot(): ResetRootEvent = resetRootEvents.awaitItem()

  /** Awaits the next [onBackStackChanged] or throws if no resets were performed. */
  suspend fun awaitOnBackStackChanged(): OnBackStackChangedEvent =
    onBackStackChangedEvents.awaitItem()

  fun assertGoToIsEmpty() {
    goToEvents.ensureAllEventsConsumed()
  }

  fun assertPopIsEmpty() {
    popEvents.ensureAllEventsConsumed()
  }

  fun assertResetRootIsEmpty() {
    resetRootEvents.ensureAllEventsConsumed()
  }

  suspend fun skipOnBackStackChanged(skip: Int) {
    onBackStackChangedEvents.skipItems(skip)
  }

  data class OnBackStackChangedEvent(val backStack: ImmutableList<Screen>)

  data class GoToEvent(val screen: Screen)

  data class PopEvent(val backStack: ImmutableList<Screen>, val result: PopResult? = null)

  data class ResetRootEvent(
    val newRoot: Screen,
    val saveState: Boolean = false,
    val restoreState: Boolean = false,
  )
}

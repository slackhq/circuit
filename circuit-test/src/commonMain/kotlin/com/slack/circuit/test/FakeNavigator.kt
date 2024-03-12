// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import app.cash.turbine.Turbine
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.foundation.Navigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.resetRoot
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/**
 * A fake [Navigator] that can be used in tests to record and assert navigation events.
 *
 * This navigator acts as a real navigator for all intents and purposes, navigating either a given
 * [BackStack] or using a simple real one under the hood if one isn't provided.
 *
 * Example
 *
 * ```kotlin
 * val navigator = FakeNavigator(FavoritesScreen)
 * val presenter = FavoritesPresenter(navigator)
 *
 * presenter.test {
 *   val state = awaitItem()
 *   state.eventSink(AddFavoriteClick)
 *   assertThat(navigator.awaitNextScreen())
 *     .isEqualTo(AddFavoriteScreen)
 * }
 * ```
 */
public class FakeNavigator internal constructor(private val delegate: Navigator) :
  Navigator by delegate {
  public constructor(
    backStack: BackStack<out BackStack.Record>
  ) : this(
    // Use a real navigator. This fake more or less just decorates it and intercepts events
    Navigator(backStack) {}
  )

  public constructor(
    root: Screen
  ) : this(
    // Use a real back stack
    SaveableBackStack(root)
  )

  private val goToEvents = Turbine<Screen>()
  private val resetRootEvents = Turbine<ResetRootEvent>()
  private val popEvents = Turbine<PopEvent>()

  override fun goTo(screen: Screen) {
    delegate.goTo(screen)
    goToEvents.add(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    val popped = delegate.pop(result)
    popEvents.add(PopEvent(popped, result))
    return popped
  }

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): ImmutableList<Screen> {
    val oldScreens = delegate.resetRoot(newRoot, saveState, restoreState)
    resetRootEvents.add(ResetRootEvent(newRoot, oldScreens, saveState, restoreState))
    return oldScreens
  }

  /**
   * Returns the next [Screen] that was navigated to or throws if no screens were navigated to.
   *
   * For non-coroutines users only.
   */
  public fun takeNextScreen(): Screen = goToEvents.takeItem()

  /** Awaits the next [Screen] that was navigated to or throws if no screens were navigated to. */
  public suspend fun awaitNextScreen(): Screen = goToEvents.awaitItem()

  /** Awaits the next navigation [resetRoot] or throws if no resets were performed. */
  public suspend fun awaitResetRoot(): ResetRootEvent = resetRootEvents.awaitItem()

  /** Awaits the next navigation [pop] event or throws if no pops are performed. */
  public suspend fun awaitPop(): PopEvent = popEvents.awaitItem()

  /** Asserts that all events so far have been consumed. */
  public fun assertIsEmpty() {
    goToEvents.ensureAllEventsConsumed()
  }

  /** Asserts that no events have been emitted. */
  public fun expectNoEvents() {
    goToEvents.expectNoEvents()
  }

  /** Represents a recorded [Navigator.pop] event. */
  public data class PopEvent(val poppedScreen: Screen?, val result: PopResult? = null)

  /** Represents a recorded [Navigator.resetRoot] event. */
  public data class ResetRootEvent(
    val newRoot: Screen,
    val oldScreens: ImmutableList<Screen>,
    val saveState: Boolean = false,
    val restoreState: Boolean = false,
  )
}

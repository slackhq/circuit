// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import app.cash.turbine.Turbine
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A fake [Navigator] that can be used in tests to record and assert navigation events.
 *
 * Example
 *
 * ```kotlin
 * val navigator = FakeNavigator()
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
public class FakeNavigator(initialScreen: Screen? = null) : Navigator {
  private val navigatedScreens = ArrayDeque<Screen>().apply { initialScreen?.let(::add) }
  private val newRoots = Turbine<Screen>()
  private val pops = Turbine<Unit>()
  private val results = Turbine<PopResult>()

  override fun goTo(screen: Screen) {
    navigatedScreens.add(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    pops.add(Unit)
    result?.let(results::add)
    return navigatedScreens.removeLastOrNull()
  }

  override fun peek(): BackStack.Record? {
    // TODO can this be implemented?
    return null
  }

  override fun resetRoot(newRoot: Screen): List<Screen> {
    newRoots.add(newRoot)
    // Note: to simulate popping off the backstack, screens should be returned in the reverse
    // order that they were added. As the channel returns them in the order they were added, we
    // need to reverse here before returning.
    val oldScreens = navigatedScreens.toList().reversed()
    navigatedScreens.clear()

    return oldScreens
  }

  /**
   * Returns the next [Screen] that was navigated to or throws if no screens were navigated to.
   *
   * For non-coroutines users only.
   */
  public fun takeNextScreen(): Screen = navigatedScreens.removeFirst()

  /** Awaits the next [Screen] that was navigated to or throws if no screens were navigated to. */
  // TODO suspend isn't necessary here anymore but left for backwards compatibility
  @Suppress("RedundantSuspendModifier")
  public suspend fun awaitNextScreen(): Screen = navigatedScreens.removeFirst()

  /** Awaits the next navigation [resetRoot] or throws if no resets were performed. */
  public suspend fun awaitResetRoot(): Screen = newRoots.awaitItem()

  /** Awaits the next navigation [pop] event or throws if no pops are performed. */
  public suspend fun awaitPop(): Unit = pops.awaitItem()

  /** Asserts that all events so far have been consumed. */
  public fun assertIsEmpty() {
    check(navigatedScreens.isEmpty())
  }

  /** Asserts that no events have been emitted. */
  public fun expectNoEvents() {
    check(navigatedScreens.isEmpty())
  }
}

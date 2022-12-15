// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import app.cash.turbine.Turbine
import com.slack.circuit.Navigator
import com.slack.circuit.Screen

/**
 * A fake [Navigator] that can be used in tests to record and assert navigation events.
 *
 * Example
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
public class FakeNavigator : Navigator {
  private val navigatedScreens = Turbine<Screen>()
  private val resets = Turbine<Screen>()
  private val pops = Turbine<Unit>()

  override fun goTo(screen: Screen) {
    navigatedScreens.add(screen)
  }

  override fun pop(): Screen? {
    pops.add(Unit)
    return null
  }

  override fun reset(newRoot: Screen): Screen? {
    resets.add(newRoot)
    return null
  }

  /**
   * Returns the next [Screen] that was navigated to or throws if no screens were navigated to.
   *
   * For non-coroutines users only.
   */
  public fun takeNextScreen(): Screen = navigatedScreens.takeItem()

  /** Awaits the next [Screen] that was navigated to or throws if no screens were navigated to. */
  public suspend fun awaitNextScreen(): Screen = navigatedScreens.awaitItem()

  /** Awaits the next navigation [reset] or throws if no resets were performed. */
  public suspend fun awaitReset(): Screen = resets.awaitItem()

  /** Awaits the next navigation [pop] event or throws if no pops are performed. */
  public suspend fun awaitPop(): Unit = pops.awaitItem()

  /** Asserts that all events so far have been consumed. */
  public fun assertIsEmpty() {
    navigatedScreens.ensureAllEventsConsumed()
  }

  /** Asserts that no events have been emitted. */
  public fun expectNoEvents() {
    navigatedScreens.expectNoEvents()
  }
}

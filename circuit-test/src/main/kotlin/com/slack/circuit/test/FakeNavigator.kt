// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.test

import app.cash.turbine.Turbine
import com.slack.circuit.NavigableScreen
import com.slack.circuit.Navigator

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
public class FakeNavigator : Navigator {
  private val navigatedScreens = Turbine<NavigableScreen>()
  private val newRoots = Turbine<NavigableScreen>()
  private val pops = Turbine<Unit>()

  override fun goTo(screen: NavigableScreen) {
    navigatedScreens.add(screen)
  }

  override fun pop(): NavigableScreen? {
    pops.add(Unit)
    return null
  }

  override fun resetRoot(newRoot: NavigableScreen): List<NavigableScreen> {
    newRoots.add(newRoot)
    val oldScreens = buildList {
      val channel = navigatedScreens.asChannel()
      while (true) {
        val screen = channel.tryReceive().getOrNull() ?: break
        add(screen)
      }
    }

    // Note: to simulate popping off the backstack, screens should be returned in the reverse
    // order that they were added. As the channel returns them in the order they were added, we
    // need to reverse here before returning.
    return oldScreens.reversed()
  }

  /**
   * Returns the next [Screen] that was navigated to or throws if no screens were navigated to.
   *
   * For non-coroutines users only.
   */
  public fun takeNextScreen(): NavigableScreen = navigatedScreens.takeItem()

  /** Awaits the next [Screen] that was navigated to or throws if no screens were navigated to. */
  public suspend fun awaitNextScreen(): NavigableScreen = navigatedScreens.awaitItem()

  /** Awaits the next navigation [resetRoot] or throws if no resets were performed. */
  public suspend fun awaitResetRoot(): NavigableScreen = newRoots.awaitItem()

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

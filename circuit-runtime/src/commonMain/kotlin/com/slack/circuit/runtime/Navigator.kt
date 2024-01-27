// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/** A Navigator that only supports [goTo]. */
@Stable
public interface GoToNavigator {
  public fun goTo(screen: Screen)
}

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
public interface Navigator : GoToNavigator {
  public override fun goTo(screen: Screen) {
    goToForResult(screen, null)
  }

  public fun goToForResult(screen: Screen, resultKey: String? = null)

  public fun pop(result: PopResult? = null): Screen?

  /** Returns the current [Screen] at the top of the back stack. */
  // TODO expose a record instead?
  // TODO should this be nullable?
  public fun peek(): Screen? = null

  public suspend fun awaitResult(key: String): PopResult?

  /**
   * Clear the existing backstack of [screens][Screen] and navigate to [newRoot].
   *
   * This is useful in preventing the user from returning to a completed workflow, such as a
   * tutorial, wizard, or authentication flow.
   *
   * Example
   *
   * ```kotlin
   * val navigator = Navigator()
   * navigator.push(LoginScreen1)
   * navigator.push(LoginScreen2)
   *
   * // Login flow is complete. Wipe backstack and set new root screen
   * val loginScreens = navigator.resetRoot(HomeScreen)
   * ```
   */
  public fun resetRoot(newRoot: Screen): List<Screen>

  public object NoOp : Navigator {
    override fun goTo(screen: Screen) {}

    override fun goToForResult(screen: Screen, resultKey: String?) {}

    override fun pop(result: PopResult?): Screen? = null

    override suspend fun awaitResult(key: String): PopResult? = null

    override fun resetRoot(newRoot: Screen): List<Screen> = emptyList()
  }
}

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
public fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}

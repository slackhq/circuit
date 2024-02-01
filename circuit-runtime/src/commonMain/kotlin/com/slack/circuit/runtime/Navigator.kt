// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.screen.Screen

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
public interface Navigator {
  public fun goTo(screen: Screen)

  public fun pop(): Screen?

  /** Returns current top most screen of backstack, or null if backstack is empty. */
  public fun peek(): Screen?

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
  public fun resetRoot(
    newRoot: Screen,
    saveState: Boolean = false,
    restoreState: Boolean = false,
  ): List<Screen>

  public object NoOp : Navigator {
    override fun goTo(screen: Screen) {}

    override fun pop(): Screen? = null

    override fun peek(): Screen? = null

    override fun resetRoot(
      newRoot: Screen,
      saveState: Boolean,
      restoreState: Boolean,
    ): List<Screen> = emptyList()
  }
}

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
public fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (peek()?.let(predicate) == false) pop()
}

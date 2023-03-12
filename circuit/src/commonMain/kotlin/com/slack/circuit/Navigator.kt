// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.CircuitUiEvent

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
public interface Navigator {
  public fun goTo(screen: Screen)

  public fun pop(): Screen?

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
    override fun pop(): Screen? = null
    override fun resetRoot(newRoot: Screen): List<Screen> = emptyList()
  }
}

/**
 * A Circuit call back to help navigate to different screens. Intended to be used when forwarding
 * [NavEvent]s from nested [Presenter]s.
 */
public fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    is GoToNavEvent -> goTo(event.screen)
    is ResetRootNavEvent -> resetRoot(event.newRoot)
    PopNavEvent -> pop()
  }
}

/** A sealed navigation interface intended to be used when making a navigation call back. */
public sealed interface NavEvent : CircuitUiEvent

internal object PopNavEvent : NavEvent

internal data class GoToNavEvent(internal val screen: Screen) : NavEvent

internal data class ResetRootNavEvent(internal val newRoot: Screen) : NavEvent

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
public fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}

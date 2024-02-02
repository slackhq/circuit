// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen

/**
 * A Circuit call back to help navigate to different screens. Intended to be used when forwarding
 * [NavEvent]s from nested [Presenter]s.
 */
public fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    NavEvent.Pop -> pop()
    is NavEvent.GoTo -> goTo(event.screen)
    is NavEvent.ResetRoot -> resetRoot(event.newRoot)
  }
}

public fun Navigator.Companion.navEventNavigator(
  screen: Screen,
  onNavEvent: (event: NavEvent) -> Unit,
): Navigator {
  return object : Navigator {
    override fun goTo(screen: Screen) {
      onNavEvent(NavEvent.GoTo(screen))
    }

    override fun resetRoot(
      newRoot: Screen,
      saveState: Boolean,
      restoreState: Boolean,
    ): List<Screen> {
      onNavEvent(NavEvent.ResetRoot(newRoot, saveState, restoreState))
      return emptyList()
    }

    override fun pop(): Screen? {
      onNavEvent(NavEvent.Pop)
      return null
    }

    override fun peek(): Screen = screen

    override fun peekBackStack(): List<Screen> = listOf(screen)
  }
}

/** A sealed navigation interface intended to be used when making a navigation callback. */
public sealed interface NavEvent : CircuitUiEvent {
  /** Corresponds to [Navigator.pop]. */
  public data object Pop : NavEvent

  /** Corresponds to [Navigator.goTo]. */
  public data class GoTo(val screen: Screen) : NavEvent

  /** Corresponds to [Navigator.resetRoot]. */
  public data class ResetRoot(
    val newRoot: Screen,
    val saveState: Boolean,
    val restoreState: Boolean,
  ) : NavEvent
}

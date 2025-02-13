// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavigationContext
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A Circuit call back to help navigate to different screens. Intended to be used when forwarding
 * [NavEvent]s from nested [Presenter]s.
 */
public fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    is NavEvent.Pop -> pop(event.result, context = event.context)
    is NavEvent.GoTo -> goTo(event.screen, context = event.context)
    is NavEvent.ResetRoot -> resetRoot(event.newRoot, context = event.context)
  }
}

/** A sealed navigation interface intended to be used when making a navigation callback. */
public sealed interface NavEvent : CircuitUiEvent {
  /** Corresponds to [Navigator.pop]. */
  public data class Pop(
    val result: PopResult? = null,
    val context: NavigationContext = NavigationContext.EMPTY,
  ) : NavEvent

  /** Corresponds to [Navigator.goTo]. */
  public data class GoTo(
    val screen: Screen,
    val context: NavigationContext = NavigationContext.EMPTY,
  ) : NavEvent

  /** Corresponds to [Navigator.resetRoot]. */
  public data class ResetRoot(
    val newRoot: Screen,
    val saveState: Boolean,
    val restoreState: Boolean,
    val context: NavigationContext = NavigationContext.EMPTY,
  ) : NavEvent
}

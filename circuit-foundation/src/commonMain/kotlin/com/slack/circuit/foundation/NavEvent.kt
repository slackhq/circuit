// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * A Circuit call back to help navigate to different screens. Intended to be used when forwarding
 * [NavEvent]s from nested [Presenter]s.
 */
public fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    is NavEvent.Pop -> pop(event.result)
    is NavEvent.GoTo -> goTo(event.screen)
    is NavEvent.ResetRoot -> resetRoot(event.newRoot, event.options)
  }
}

/** A sealed navigation interface intended to be used when making a navigation callback. */
public sealed interface NavEvent : CircuitUiEvent {
  /** Corresponds to [Navigator.pop]. */
  public data class Pop(val result: PopResult? = null) : NavEvent

  /** Corresponds to [Navigator.goTo]. */
  public data class GoTo(val screen: Screen) : NavEvent

  /** Corresponds to [Navigator.resetRoot]. */
  public data class ResetRoot(val newRoot: Screen, val options: StateOptions) : NavEvent {
    @Deprecated(
      message = "Use the StateOptions variant",
      replaceWith =
        ReplaceWith(
          "ResetRoot(newRoot, StateOptions(save = save, restore = restore, clear = clear))",
          "com.slack.circuit.runtime.Navigator.StateOptions",
        ),
    )
    public constructor(
      newRoot: Screen,
      save: Boolean,
      restore: Boolean,
    ) : this(newRoot, StateOptions(save = save, restore = restore, clear = false))
  }
}

package com.slack.circuit

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.Screen


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
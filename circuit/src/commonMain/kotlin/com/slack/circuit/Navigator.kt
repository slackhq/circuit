// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Stable

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
public interface Navigator {
  public fun goTo(screen: Screen)

  public fun pop(): Screen?

  public object NoOp : Navigator {
    override fun goTo(screen: Screen) {}
    override fun pop(): Screen? = null
  }
}

/**
 * A Circuit call back to help navigate to different screens. Intended to be used when forwarding
 * [ChildScreenEvent]s from nested [Presenter]s.
 */
public fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    is GoToNavEvent -> goTo(event.screen)
    PopNavEvent -> pop()
  }
}

/** A sealed navigation interface intended to be used when making a navigation or result call back. */
public sealed interface ChildScreenEvent : CircuitUiEvent

public sealed interface NavEvent : ChildScreenEvent

internal object PopNavEvent : NavEvent

internal data class GoToNavEvent(internal val screen: Screen) : NavEvent

internal data class ScreenResultEvent(val result: ScreenResult?) : ChildScreenEvent

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
public fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}

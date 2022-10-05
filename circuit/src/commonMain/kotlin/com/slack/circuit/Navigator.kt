/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * [NavEvent]s from nested [Presenter]s.
 */
public fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    is GoToNavEvent -> goTo(event.screen)
    PopNavEvent -> pop()
  }
}

/** A sealed navigation interface intended to be used when making a navigation call back. */
public sealed interface NavEvent : CircuitUiEvent

internal object PopNavEvent : NavEvent

internal data class GoToNavEvent(internal val screen: Screen) : NavEvent

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
public fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}

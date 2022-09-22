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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.SaveableBackStack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/** A basic navigation interface for navigating between [screens][Screen]. */
@Stable
interface Navigator {
  fun goTo(screen: Screen)

  fun pop(): Screen?

  object NoOp : Navigator {
    override fun goTo(screen: Screen) {}
    override fun pop(): Screen? = null
  }
}

/** A Circuit call back to help navigate to different screens. */
fun Navigator.onNavEvent(event: NavEvent) {
  when (event) {
    is GoToNavEvent -> goTo(event.screen)
    PopNavEvent -> pop()
  }
}

/**
 * Helper to collect the Events from a [Presenter], filter out the [NavEvent], then navigate to the
 * intended [Screen].
 *
 * @param navigator The injected [Navigator].
 * @param events The [events][UiEvent] being passed in from the [Presenter].
 */
@Composable
inline fun <reified E : CircuitUiEvent, reified NavEventHolder : E> NavEventCollector(
  events: Flow<E>,
  crossinline mapper: (NavEventHolder) -> NavEvent,
  crossinline collector: (NavEvent) -> Unit
) {
  val rememberChildNavigationEvent = remember {
    events.filterIsInstance<NavEventHolder>().map(mapper)
  }

  EventCollector(rememberChildNavigationEvent) { event -> collector(event) }
}

/** A sealed navigation interface intended to be used when making a navigation call back. */
sealed interface NavEvent : CircuitUiEvent

internal object PopNavEvent : NavEvent

internal data class GoToNavEvent(internal val screen: Screen) : NavEvent

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @see NavigableCircuitContent
 *
 * @param backstack The backing [SaveableBackStack] to navigate.
 * @param onRootPop The callback to handle root [Navigator.pop] calls.
 */
@Composable
fun rememberCircuitNavigator(backstack: SaveableBackStack, onRootPop: (() -> Unit)?): Navigator {
  return remember { NavigatorImpl(backstack, onRootPop) }
}

private class NavigatorImpl(
  private val backstack: SaveableBackStack,
  private val onRootPop: (() -> Unit)?,
) : Navigator {

  override fun goTo(screen: Screen) {
    backstack.push(screen)
  }

  override fun pop(): Screen? {
    backstack.pop()?.screen?.let {
      return it
    }
    onRootPop?.invoke()
    return null
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as NavigatorImpl

    if (backstack != other.backstack) return false
    if (onRootPop != other.onRootPop) return false

    return true
  }

  override fun hashCode(): Int {
    var result = backstack.hashCode()
    result = 31 * result + (onRootPop?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "NavigatorImpl(backstack=$backstack, onRootPop=$onRootPop)"
  }
}

/** Calls [Navigator.pop] until the given [predicate] is matched or it pops the root. */
fun Navigator.popUntil(predicate: (Screen) -> Boolean) {
  while (true) {
    val screen = pop() ?: break
    if (predicate(screen)) {
      break
    }
  }
}

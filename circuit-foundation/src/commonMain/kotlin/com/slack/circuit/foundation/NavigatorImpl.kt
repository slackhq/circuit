// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.isEmpty
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

/**
 * Creates and remembers a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @param backStack The backing [BackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backStack: BackStack<out Record>,
  onRootPop: (result: PopResult?) -> Unit,
): Navigator {
  return remember { Navigator(backStack, onRootPop) }
}

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent] while also
 * handling back events with a [BackHandler].
 *
 * @param backStack The backing [BackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @param enableBackHandler Indicates whether or not [Navigator.pop] should be called by the system
 *   back handler. Defaults to true.
 * @see NavigableCircuitContent
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
public fun rememberCircuitNavigator(
  backStack: BackStack<out Record>,
  onRootPop: (result: PopResult?) -> Unit,
  enableBackHandler: Boolean = true,
): Navigator {
  var trigger by remember { mutableStateOf(false) }
  val navigator = rememberCircuitNavigator(backStack = backStack, onRootPop = onRootPop)
  BackHandler(
    enabled = enableBackHandler && !trigger && backStack.size > 1,
    onBack = {
      // We need to unload this BackHandler from the composition before the root pop is triggered so
      // any outer back handler will get called. So delay calling pop until after the next
      // composition.
      if (backStack.size > 1) {
        navigator.pop()
      } else {
        trigger = true
      }
    },
  )
  if (trigger) {
    SideEffect {
      navigator.pop()
      trigger = false
    }
  }
  return navigator
}

/**
 * Creates a new [Navigator].
 *
 * @param backStack The backing [BackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
public fun Navigator(
  backStack: BackStack<out Record>,
  onRootPop: (result: PopResult?) -> Unit,
): Navigator = NavigatorImpl(backStack, onRootPop)

internal class NavigatorImpl(
  private val backStack: BackStack<out Record>,
  private val onRootPop: (result: PopResult?) -> Unit,
) : Navigator {

  init {
    check(!backStack.isEmpty) { "Backstack size must not be empty." }
  }

  override fun goTo(screen: Screen): Boolean {
    return backStack.push(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    if (backStack.isAtRoot) {
      onRootPop(result)
      return null
    }

    return backStack.pop(result)?.screen
  }

  override fun peek(): Screen? = backStack.firstOrNull()?.screen

  override fun peekBackStack(): ImmutableList<Screen> = backStack.mapToImmutableList { it.screen }

  override fun resetRoot(
    newRoot: Screen,
    saveState: Boolean,
    restoreState: Boolean,
  ): ImmutableList<Screen> {
    // Run this in a mutable snapshot (bit like a transaction)
    val currentStack =
      Snapshot.withMutableSnapshot {
        if (saveState) backStack.saveState()
        // Pop everything off the back stack
        val popped = backStack.popUntil { false }.mapToImmutableList { it.screen }

        // If we're not restoring state, or the restore didn't work, we need to push the new root
        // onto the stack
        if (!restoreState || !backStack.restoreState(newRoot)) {
          backStack.push(newRoot)
        }
        popped
      }

    return currentStack
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as NavigatorImpl

    if (backStack != other.backStack) return false
    if (onRootPop != other.onRootPop) return false

    return true
  }

  override fun hashCode(): Int {
    var result = backStack.hashCode()
    result = 31 * result + onRootPop.hashCode()
    return result
  }

  override fun toString(): String {
    return "NavigatorImpl(backStack=$backStack, onRootPop=$onRootPop)"
  }
}

private inline fun <T, R> Iterable<T>.mapToImmutableList(transform: (T) -> R): ImmutableList<R> {
  return persistentListOf<R>().mutate {
    for (element in this@mapToImmutableList) {
      it.add(transform(element))
    }
  }
}

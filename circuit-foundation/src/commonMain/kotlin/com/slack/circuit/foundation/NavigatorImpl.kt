// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.isEmpty
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavigationContext
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

  override fun goTo(screen: Screen, context: NavigationContext): Boolean {
    return backStack.push(screen, context = context)
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
    context: NavigationContext,
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
          backStack.push(newRoot, context = context)
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

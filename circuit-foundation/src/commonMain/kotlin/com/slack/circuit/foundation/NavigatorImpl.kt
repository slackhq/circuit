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
import com.slack.circuit.runtime.screen.Screen

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @param backstack The backing [BackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backstack: BackStack<out Record>,
  onRootPop: () -> Unit,
): Navigator {
  return remember { NavigatorImpl(backstack, onRootPop) }
}

internal class NavigatorImpl(
  private val backstack: BackStack<out Record>,
  private val onRootPop: () -> Unit,
) : Navigator {

  init {
    check(!backstack.isEmpty) { "Backstack size must not be empty." }
  }

  override fun goTo(screen: Screen) = backstack.push(screen)

  override fun pop(): Screen? {
    if (backstack.isAtRoot) {
      onRootPop()
      return null
    }

    return backstack.pop()?.screen
  }

  override fun peek(): Screen? = backstack.firstOrNull()?.screen

  override fun peekBackStack(): List<Screen> = backstack.map { it.screen }

  override fun resetRoot(newRoot: Screen, saveState: Boolean, restoreState: Boolean): List<Screen> {
    val currentStack = backstack.map(Record::screen)

    // Run this in a mutable snapshot (bit like a transaction)
    Snapshot.withMutableSnapshot {
      if (saveState) backstack.saveState()
      // Pop everything off the back stack
      backstack.popUntil { false }

      // If we're not restoring state, or the restore didn't work, we need to push the new root
      // onto the stack
      if (!restoreState || !backstack.restoreState(newRoot)) {
        backstack.push(newRoot)
      }
    }

    return currentStack
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as NavigatorImpl

    if (backstack != other.backstack) return false
    if (onRootPop != other.onRootPop) return false

    return true
  }

  override fun hashCode(): Int {
    var result = backstack.hashCode()
    result = 31 * result + onRootPop.hashCode()
    return result
  }

  override fun toString(): String {
    return "NavigatorImpl(backstack=$backstack, onRootPop=$onRootPop)"
  }
}

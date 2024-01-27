// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.isEmpty
import com.slack.circuit.runtime.DelicateCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @param backStack The backing [BackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backStack: BackStack<out Record>,
  onRootPop: () -> Unit,
): Navigator {
  return remember { NavigatorImpl(backStack, onRootPop) }
}

@OptIn(DelicateCircuitApi::class)
internal class NavigatorImpl(
  override val backStack: BackStack<out Record>,
  private val onRootPop: () -> Unit,
) : Navigator {

  init {
    check(!backStack.isEmpty) { "Backstack size must not be empty." }
  }

  override fun goTo(screen: Screen) {
    backStack.push(screen)
  }

  override fun pop(result: PopResult?): Screen? {
    if (backStack.isAtRoot) {
      onRootPop()
      return null
    }

    return backStack.pop(result)?.screen
  }

  override fun resetRoot(newRoot: Screen): List<Screen> {
    return buildList(backStack.size) {
      backStack.popUntil { record ->
        add(record.screen)
        false
      }
      backStack.push(newRoot)
    }
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

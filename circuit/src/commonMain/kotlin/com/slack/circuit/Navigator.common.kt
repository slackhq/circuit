// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.isEmpty
import com.slack.circuit.backstack.popUntil

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @param backstack The backing [SaveableBackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backstack: SaveableBackStack,
  onRootPop: () -> Unit,
): Navigator {
  return remember { NavigatorImpl(backstack, onRootPop) }
}

internal class NavigatorImpl(
  private val backstack: SaveableBackStack,
  private val onRootPop: () -> Unit
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

  override fun resetRoot(newRoot: Screen): List<Screen> {
    return buildList(backstack.size) {
      backstack.popUntil { record ->
        add(record.screen)
        false
      }
      backstack.push(newRoot)
    }
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
    result = 31 * result + onRootPop.hashCode()
    return result
  }

  override fun toString(): String {
    return "NavigatorImpl(backstack=$backstack, onRootPop=$onRootPop)"
  }
}

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
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isEmpty

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 *
 * @see NavigableCircuitContent
 *
 * @param backstack The backing [SaveableBackStack] to navigate.
 * @param onRootPop A callback to handle when the [backstack] is [popped][Navigator.pop] to empty.
 */
@Composable
public fun rememberCircuitNavigator(
  backstack: SaveableBackStack,
  onRootPop: (() -> Unit)
): Navigator {
  return remember { NavigatorImpl(backstack, onRootPop) }
}

internal class NavigatorImpl(
  private val backstack: SaveableBackStack,
  private val onRootPop: (() -> Unit),
) : Navigator {

  init {
    check(!backstack.isEmpty) { "Backstack size must not be empty." }
  }

  override fun goTo(screen: Screen) {
    backstack.push(screen)
  }

  override fun pop(): Screen? {
    val screen = backstack.pop()?.screen
    if (backstack.size == 0) {
      onRootPop()
    }
    return screen
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

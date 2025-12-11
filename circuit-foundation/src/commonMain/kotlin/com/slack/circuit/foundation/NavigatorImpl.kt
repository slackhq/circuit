// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.navigation.NavStack
import com.slack.circuit.runtime.navigation.NavStack.Record
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.isAtRoot
import com.slack.circuit.runtime.navigation.isEmpty
import com.slack.circuit.runtime.navigation.transform
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * Creates and remembers a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 * A new [Navigator] will be created if the [backStack] instance changes.
 *
 * @param backStack The backing [BackStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backStack: BackStack<out BackStack.Record>,
  onRootPop: (result: PopResult?) -> Unit,
): Navigator = rememberCircuitNavigator(navStack = backStack, onRootPop = onRootPop)

/**
 * Creates and remembers a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 * A new [Navigator] will be created if the [navStack] instance changes.
 *
 * @param navStack The backing [NavStack] to navigate.
 * @param onRootPop Invoked when the backstack [NavStack.isAtRoot] and a [Navigator.pop] is called.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  navStack: NavStack<out Record>,
  onRootPop: (result: PopResult?) -> Unit,
): Navigator {
  val latestOnRootPop by rememberUpdatedState(onRootPop)
  return remember(navStack) { Navigator(navStack) { popResult -> latestOnRootPop(popResult) } }
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
  backStack: BackStack<out BackStack.Record>,
  onRootPop: (result: PopResult?) -> Unit,
  enableBackHandler: Boolean = true,
): Navigator =
  rememberCircuitNavigator(
    navStack = backStack,
    onRootPop = onRootPop,
    enableBackHandler = enableBackHandler,
  )

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent] while also
 * handling back events with a [BackHandler].
 *
 * @param navStack The backing [NavStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @param enableBackHandler Indicates whether or not [Navigator.pop] should be called by the system
 *   back handler. Defaults to true.
 * @see NavigableCircuitContent
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
public fun rememberCircuitNavigator(
  navStack: NavStack<out Record>,
  onRootPop: (result: PopResult?) -> Unit,
  enableBackHandler: Boolean = true,
): Navigator {
  val navigator = rememberCircuitNavigator(navStack = navStack, onRootPop = onRootPop)
  // Check the screen and not the record as `popRoot()` reorders the screens creating new records.
  // Also `popUntil` can run to a null screen, which we want to treat as the last screen.
  val hasScreenChanged = remember {
    var lastScreen: Screen? = navigator.peek()
    derivedStateOf {
      val screen = navigator.peek()
      if (screen != null && screen != lastScreen) {
        lastScreen = screen
      }
      lastScreen
    }
  }
  var hasPendingRootPop by remember(hasScreenChanged) { mutableStateOf(false) }
  var enableRootBackHandler by remember(hasScreenChanged) { mutableStateOf(true) }
  BackHandler(
    enabled = enableBackHandler && enableRootBackHandler && !navStack.isAtRoot,
    onBack = {
      // We need to unload this BackHandler from the composition before the root pop is triggered so
      // any outer back handler will get called. So delay calling pop until after the next
      // composition.
      if (!navStack.isAtRoot) {
        navigator.pop()
      } else {
        hasPendingRootPop = true
        enableRootBackHandler = false
      }
    },
  )
  if (hasPendingRootPop) {
    SideEffect {
      navigator.pop()
      hasPendingRootPop = false
    }
  }
  return navigator
}

/**
 * Creates a new [Navigator].
 *
 * @param navStack The backing [NavStack] to navigate.
 * @param onRootPop Invoked when the backstack is at root (size 1) and the user presses the back
 *   button.
 * @see NavigableCircuitContent
 */
public fun Navigator(
  navStack: NavStack<out Record>,
  onRootPop: (result: PopResult?) -> Unit,
): Navigator = NavigatorImpl(navStack, onRootPop)

internal class NavigatorImpl(
  private val navStack: NavStack<out Record>,
  private val onRootPop: (result: PopResult?) -> Unit,
) : Navigator {

  init {
    check(!navStack.isEmpty) { "NavStack size must not be empty." }
  }

  override fun goTo(screen: Screen): Boolean {
    return navStack.push(screen)
  }

  override fun forward(): Boolean {
    return navStack.forward()
  }

  override fun backward(): Boolean {
    return navStack.backward()
  }

  override fun pop(result: PopResult?): Screen? {
    if (navStack.isAtRoot) {
      onRootPop(result)
      return null
    }
    return navStack.pop()?.screen
  }

  override fun peek(): Screen? = navStack.currentRecord?.screen

  override fun peekNavStack(): NavStackList<Screen>? = navStack.snapshot()?.transform { it.screen }

  override fun peekBackStack(): List<Screen> =
    peekNavStack()?.run {
      buildList {
        add(active)
        addAll(backwardItems)
      }
    } ?: emptyList()

  override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> {
    // Run this in a mutable snapshot (bit like a transaction)
    val currentStack =
      Snapshot.withMutableSnapshot {
        if (options.save) navStack.saveState()
        // Pop everything off the back stack
        val popped = buildList {
          while (navStack.size > 0) {
            val screen = navStack.pop()?.screen ?: break
            add(screen)
          }
        }

        // If we're not restoring state, or the restore didn't work, we need to push the new root
        // onto the stack
        if (!options.restore || !navStack.restoreState(newRoot)) {
          navStack.push(newRoot)
        }

        // Clear the state if requested, do this last to allow restoring the state once.
        if (options.clear) navStack.removeState(newRoot)
        popped
      }

    return currentStack
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as NavigatorImpl

    if (navStack != other.navStack) return false
    if (onRootPop != other.onRootPop) return false

    return true
  }

  override fun hashCode(): Int {
    var result = navStack.hashCode()
    result = 31 * result + onRootPop.hashCode()
    return result
  }

  override fun toString(): String {
    return "NavigatorImpl(navStack=$navStack, onRootPop=$onRootPop)"
  }
}

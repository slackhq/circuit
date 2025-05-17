// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.BackStack.Record
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent]. Delegates
 * onRootPop to the [LocalOnBackPressedDispatcherOwner].
 *
 * @param backStack The backing [BackStack] to navigate.
 * @param enableBackHandler Indicates whether or not [Navigator.pop] should be called by the system
 *   back handler. Defaults to true.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backStack: BackStack<out Record>,
  enableBackHandler: Boolean = true,
): Navigator {
  return rememberCircuitNavigator(
    backStack = backStack,
    onRootPop = backDispatcherRootPop(),
    enableBackHandler = enableBackHandler,
  )
}

@Composable
private fun backDispatcherRootPop(): (result: PopResult?) -> Unit {
  val onBackPressedDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("No OnBackPressedDispatcherOwner found, unable to handle root navigation pops.")
  return { onBackPressedDispatcher.onBackPressed() }
}

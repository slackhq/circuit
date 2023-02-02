// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.SaveableBackStack

/**
 * Returns a new [Navigator] for navigating within [CircuitContents][CircuitContent].
 * Delegates onRootPop to the [LocalOnBackPressedDispatcherOwner].
 *
 * @param backstack The backing [SaveableBackStack] to navigate.
 * @see NavigableCircuitContent
 */
@Composable
public fun rememberCircuitNavigator(
  backstack: SaveableBackStack,
): Navigator {
  return rememberCircuitNavigator(
    backstack = backstack,
    onRootPop = backDispatcherRootPop(),
  )
}

@Composable
private fun backDispatcherRootPop(): () -> Unit {
  val onBackPressedDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("No OnBackPressedDispatcherOwner found, unable to handle root navigation pops.")
  return onBackPressedDispatcher::onBackPressed
}
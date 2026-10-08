// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Represents the type of navigation event that occurred within the context of an
 * [AnimatedNavDecoration]. This is used to drive transitions and other navigation-related behaviors
 * within the [AnimatedNavDecoration].
 *
 * @see AnimatedNavDecoration
 */
public enum class AnimatedNavEvent {
  GoTo,
  Pop,
  RootReset,
  Forward,
  Backward,
}

/**
 * Classifies the navigation from [initialStack] to [targetStack], or returns null when the active
 * item did not change.
 */
@InternalCircuitApi
public fun determineAnimatedNavEvent(
  initialStack: NavStackList<*>,
  targetStack: NavStackList<*>,
): AnimatedNavEvent? {
  val previous = initialStack.active
  val current = targetStack.active

  val initialBackStack = initialStack.backwardItems
  val initialForwardStack = initialStack.forwardItems

  val targetForwardStack = targetStack.forwardItems

  return when {
    // Root reset happened.
    initialStack.root != targetStack.root -> AnimatedNavEvent.RootReset
    // Target screen has not changed.
    previous == current -> null
    // Navigated backward with the screen moving to the forward stack.
    current in initialBackStack &&
      previous !in initialForwardStack &&
      previous in targetForwardStack -> AnimatedNavEvent.Backward
    // Popped the screen off the nav stack.
    current in initialBackStack && previous !in targetForwardStack -> AnimatedNavEvent.Pop
    // Navigated forward with the screen moving out of the forward stack.
    current in initialForwardStack && current !in targetForwardStack -> AnimatedNavEvent.Forward
    // Fallback to a normal GoTo.
    else -> AnimatedNavEvent.GoTo
  }
}

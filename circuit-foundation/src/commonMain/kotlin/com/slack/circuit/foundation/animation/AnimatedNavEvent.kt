// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

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
  Backward,
  Forward,
}

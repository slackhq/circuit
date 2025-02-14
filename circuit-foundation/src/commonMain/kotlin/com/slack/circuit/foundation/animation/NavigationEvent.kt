// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.core.Transition

public enum class NavigationEvent {
  /** Going to the [Transition.targetState] from the [Transition.currentState] */
  GoTo,
  /** Popping from the [Transition.currentState] back to to the [Transition.targetState] */
  Pop,
  /** The back stack has been reset to the [Transition.targetState]. */
  RootReset,
}

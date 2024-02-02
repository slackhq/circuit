// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Immutable

internal fun <T> Transition<T>.isStateBeingAnimated(equals: (T) -> Boolean): Boolean {
  return isRunning && (equals(currentState) || equals(targetState))
}

@Immutable
internal data class GestureNavTransitionHolder<T>(
  val record: T,
  val backStackDepth: Int,
  val rootRecord: T,
)

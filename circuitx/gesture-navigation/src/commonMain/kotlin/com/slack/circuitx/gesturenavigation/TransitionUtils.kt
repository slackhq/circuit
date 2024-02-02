// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Immutable

internal fun <T> Transition<T>.isStateBeingAnimated(equals: (T) -> Boolean): Boolean {
  return isRunning && (equals(currentState) || equals(targetState))
}

/**
 * A holder class used by the `AnimatedContent` composables. This enables us to pass through all of
 * the necessary information as an argument, which is optimal for `AnimatedContent`.
 */
@Immutable
internal data class GestureNavTransitionHolder<T>(
  val record: T,
  val backStackDepth: Int,
  val rootRecord: T,
)

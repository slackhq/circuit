// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Immutable
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.screen.Screen

internal fun <T> Transition<T>.isStateBeingAnimated(equals: (T) -> Boolean): Boolean {
  return isRunning && (equals(currentState) || equals(targetState))
}

internal val Transition<*>.isPending: Boolean
  get() = this.currentState != this.targetState

/**
 * A holder class used by the `AnimatedContent` composables. This enables us to pass through all of
 * the necessary information as an argument, which is optimal for `AnimatedContent`.
 */
@Immutable
internal data class GestureNavTransitionHolder<T : NavArgument>(
  val record: T,
  override val backStackDepth: Int,
  val rootRecord: T,
) : AnimatedNavState {
  override val screen: Screen = record.screen
  override val rootScreen: Screen = rootRecord.screen
}

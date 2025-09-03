// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.Immutable
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavState
import kotlinx.collections.immutable.ImmutableList

/**
 * A holder class used by the `AnimatedContent` composables. This enables us to pass through all of
 * the necessary information as an argument, which is optimal for `AnimatedContent`.
 */
@Immutable
internal data class GestureNavTransitionHolder<T : NavArgument>(val args: ImmutableList<T>) :
  AnimatedNavState {
  override val backStack: ImmutableList<NavArgument> = args
}

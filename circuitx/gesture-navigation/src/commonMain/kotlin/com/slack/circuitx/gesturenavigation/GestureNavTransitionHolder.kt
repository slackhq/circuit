// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.Immutable
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * A holder class used by the `AnimatedContent` composables. This enables us to pass through all of
 * the necessary information as an argument, which is optimal for `AnimatedContent`.
 */
@Immutable
internal data class GestureNavTransitionHolder<T : NavArgument>(
  val args: ImmutableList<T>,
  override val backStackDepth: Int,
) : AnimatedNavState {
  override val screen: Screen = args.first().screen
  override val rootScreen: Screen = args.last().screen
  override val backStack: ImmutableList<Screen> = args.map { it.screen }.toImmutableList()
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.Immutable
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.NavStackList

/**
 * A holder class used by the `AnimatedContent` composables. This enables us to pass through all of
 * the necessary information as an argument, which is optimal for `AnimatedContent`.
 */
@Immutable
public data class GestureNavTransitionHolder<T : NavArgument>(
  override val navStack: NavStackList<T>
) : AnimatedNavState

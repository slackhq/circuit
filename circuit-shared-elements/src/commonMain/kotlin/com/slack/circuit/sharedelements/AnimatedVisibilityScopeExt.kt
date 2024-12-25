// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sharedelements

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf

/** Current progress fraction of the animation, between 0f and 1f. */
public fun AnimatedVisibilityScope.progress(): FloatState {
  return derivedStateOf {
      with(transition) {
        when {
          isRunning || isSeeking -> {
            val fraction = playTimeNanos * 1f / totalDurationNanos
            fraction.coerceIn(0f, 1f)
          }
          currentState == EnterExitState.Visible -> 1f
          else -> 0f
        }
      }
    }
    .asFloatState()
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/** Checks whether or not we can retain, usually derived from the current composable context. */
@Stable
public fun interface CanRetainChecker {
  public fun canRetain(registry: RetainedStateRegistry): Boolean

  public companion object {
    public val Always: CanRetainChecker = CanRetainChecker { _ -> true }
  }
}

public val LocalCanRetainChecker: ProvidableCompositionLocal<CanRetainChecker?> =
  staticCompositionLocalOf {
    null
  }

@Composable public expect fun rememberCanRetainChecker(): CanRetainChecker

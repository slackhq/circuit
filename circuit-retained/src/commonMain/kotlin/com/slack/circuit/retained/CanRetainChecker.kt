// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.retained

import androidx.compose.runtime.Stable

/** Checks whether or not we can retain, usually derived from the current composable context. */
@Stable
public fun interface CanRetainChecker {
  public fun canRetain(): Boolean

  public companion object {
    public val Always: CanRetainChecker = CanRetainChecker { true }
    public val Never: CanRetainChecker = CanRetainChecker { false }
  }
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION")

package com.slack.circuit.retained

import androidx.compose.runtime.Stable

/** Checks whether or not we can retain, usually derived from the current composable context. */
@Stable
@Deprecated(RETAINED_STATE_REGISTRY_DEPRECATION)
public fun interface CanRetainChecker {
  public fun canRetain(): Boolean

  public companion object {
    public val Always: CanRetainChecker = CanRetainChecker { true }
    public val Never: CanRetainChecker = CanRetainChecker { false }
  }
}

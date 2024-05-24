// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
public interface Lifecycle {
  public val isResumed: Boolean
}

internal class MutableLifecycle(isResumed: Boolean = false) : Lifecycle {
  override var isResumed: Boolean by mutableStateOf(isResumed)
}

public val LocalLifecycle: ProvidableCompositionLocal<Lifecycle> = staticCompositionLocalOf {
  staticLifecycle(true)
}

private fun staticLifecycle(isResumed: Boolean): Lifecycle =
  object : Lifecycle {
    override val isResumed: Boolean = isResumed
  }

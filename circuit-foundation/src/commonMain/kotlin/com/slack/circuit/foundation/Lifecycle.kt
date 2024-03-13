// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

public interface Lifecycle {
  public val isPaused: Boolean
}

internal class LifecycleImpl : Lifecycle {
  internal var _isPaused by mutableStateOf(false)
  override val isPaused: Boolean
    get() = _isPaused
}

public val LocalLifecycle: ProvidableCompositionLocal<Lifecycle> = staticCompositionLocalOf {
  object : Lifecycle {
    override val isPaused: Boolean = false
  }
}

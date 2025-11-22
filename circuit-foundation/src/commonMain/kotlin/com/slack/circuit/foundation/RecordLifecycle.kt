// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Represents the lifecycle of a navigation record in a [NavigableCircuitContent]. Will typically be
 * stored in the [LocalRecordLifecycle] composition local, allowing presenters and UIs to observe
 * whether the navigation record is active.
 */
@Stable
public interface RecordLifecycle {
  /**
   * Whether the record is currently active. Typically this will return true when the record is the
   * top record in the back stack.
   */
  public val isActive: Boolean
}

internal class MutableRecordLifecycle(initial: Boolean = false) : RecordLifecycle {
  override var isActive: Boolean by mutableStateOf(initial)
}

/**
 * Holds the current lifecycle for a record in a [NavigableCircuitContent].
 *
 * For static [CircuitContent]s used outside of [NavigableCircuitContent], the default value will be
 * a [RecordLifecycle] which always returned active.
 */
public val LocalRecordLifecycle: ProvidableCompositionLocal<RecordLifecycle> =
  staticCompositionLocalOf {
    staticRecordLifecycle(true)
  }

public fun staticRecordLifecycle(isActive: Boolean): RecordLifecycle =
  object : RecordLifecycle {
    override val isActive: Boolean = isActive
  }


internal object UnsetRecordLifecycle : RecordLifecycle {
  override val isActive: Boolean = false
}

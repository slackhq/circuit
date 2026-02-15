// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
private fun rememberUpdatedRecordLifecycle(isActive: Boolean): RecordLifecycle {
  return remember { MutableRecordLifecycle() }.apply { this.isActive = isActive }
}

/**
 * Provides a [RecordLifecycle] for the current record via [LocalRecordLifecycle].
 *
 * If a [RecordLifecycle] has already been set by an outer call, this is a no-op and simply calls
 * [content] directly. The first caller wins, making it safe to call redundantly.
 *
 * By default, [NavigableCircuitContent] provides the [RecordLifecycle] directly around the record
 * content and only considers the current record active. [NavDecoration] implementations can use
 * this to override that behaviour and mark multiple records as active at once.
 *
 * @param isActive whether this record should be considered active.
 * @param content the composable content to provide the lifecycle to.
 */
@Composable
public fun ProvideRecordLifecycle(isActive: Boolean, content: @Composable () -> Unit) {
  val lifecycle =
    when (LocalRecordLifecycleState.current) {
      RecordLifecycleState.Set -> LocalRecordLifecycle.current
      RecordLifecycleState.Unset -> rememberUpdatedRecordLifecycle(isActive)
    }
  CompositionLocalProvider(
    LocalRecordLifecycle provides lifecycle,
    LocalRecordLifecycleState provides RecordLifecycleState.Set,
  ) {
    content()
  }
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

private fun staticRecordLifecycle(isActive: Boolean): RecordLifecycle =
  object : RecordLifecycle {
    override val isActive: Boolean = isActive
  }

internal val LocalRecordLifecycleState: ProvidableCompositionLocal<RecordLifecycleState> =
  compositionLocalOf {
    RecordLifecycleState.Set
  }

/** Represents the current state of the available [LocalRecordLifecycle]. */
internal enum class RecordLifecycleState {
  /** Indicates that no [RecordLifecycle] has been set for the current record. */
  Unset,

  /** Indicates that a [RecordLifecycle] has been set for the current record. */
  Set,
}

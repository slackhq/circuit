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
import com.slack.circuit.runtime.ExperimentalCircuitApi

/**
 * Represents the lifecycle of a navigation record in a [NavigableCircuitContent]. Will typically be
 * stored in the [LocalRecordLifecycle] composition local, allowing presenters and UIs to observe
 * whether the navigation record is active.
 */
@Stable
public interface RecordLifecycle {
  /**
   * Whether the record is currently active. Typically this will return true when the record is the
   * top record in the back stack and its parent record, if any, is active.
   */
  public val isActive: Boolean

  /**
   * Whether the record is shown to the user, as reported by the [NavDecoration] through
   * [ProvideRecordLifecycle]. It can differ from [isActive]: a record kept active off screen is not
   * visible, and an unfocused pane can be visible but not active.
   *
   * Decorations that don't provide a lifecycle, including the built-in ones, fall back to whether
   * the record is the top record in the back stack and its parent record, if any, is visible.
   * Records drawn mid-transition read as not visible there. Implementations that don't override
   * this return [isActive].
   */
  @ExperimentalCircuitApi
  public val isVisible: Boolean
    get() = isActive
}

@OptIn(ExperimentalCircuitApi::class)
internal class MutableRecordLifecycle(initial: Boolean = false) : RecordLifecycle {
  override var isActive: Boolean by mutableStateOf(initial)
  override var isVisible: Boolean by mutableStateOf(initial)
}

@OptIn(ExperimentalCircuitApi::class)
@Composable
internal fun rememberUpdatedRecordLifecycle(
  isActive: Boolean,
  isVisible: Boolean = isActive,
): RecordLifecycle {
  return remember { MutableRecordLifecycle() }
    .apply {
      this.isActive = isActive
      this.isVisible = isVisible
    }
}

/**
 * Provides a [RecordLifecycle] for the current record via [LocalRecordLifecycle].
 *
 * The provided lifecycle replaces the one [NavigableCircuitContent] derives from the parent record,
 * and [RecordLifecycle.isVisible] mirrors [isActive]. Use the overload taking `isVisible` to set it
 * separately.
 *
 * @param isActive whether this record should be considered active.
 * @param content the composable content to provide the lifecycle to.
 */
@OptIn(ExperimentalCircuitApi::class)
@Composable
public fun ProvideRecordLifecycle(isActive: Boolean, content: @Composable () -> Unit) {
  ProvideRecordLifecycle(isActive = isActive, isVisible = isActive, content = content)
}

/**
 * Provides a [RecordLifecycle] for the current record via [LocalRecordLifecycle].
 *
 * The provided lifecycle replaces the one [NavigableCircuitContent] derives from the parent record,
 * so combine with [LocalRecordLifecycle] to keep the parent's state.
 *
 * @param isActive whether this record should be considered active.
 * @param isVisible whether this record is shown to the user. May differ from [isActive].
 * @param content the composable content to provide the lifecycle to.
 */
@ExperimentalCircuitApi
@Composable
public fun ProvideRecordLifecycle(
  isActive: Boolean,
  isVisible: Boolean,
  content: @Composable () -> Unit,
) {
  val lifecycle = rememberUpdatedRecordLifecycle(isActive, isVisible)
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

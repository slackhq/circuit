// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.slack.circuit.runtime.screen.CircuitSaver.Companion.NoOp

/**
 * Converts [Screen]s and [PopResult]s to and from representations that can be stored in a Compose
 * `SaveableStateRegistry`.
 *
 * Circuit's saveable back/nav stack implementations use this to persist navigation state across
 * configuration changes and process death. [Screen] itself does not require any particular
 * serialization mechanism, implementations of this interface supply one.
 *
 * Available strategies include
 * - Android `Parcelable` (the Android default)
 * - kotlinx-serialization (via the `circuit-serialization` artifact)
 * - no persistence at all ([NoOp]).
 *
 * Returned values must be storable in the platform's `SaveableStateRegistry`. On Android that means
 * Bundle-supported types like `Parcelable` or `SavedState`. Other platforms hold saved state in
 * memory and accept any value.
 */
// TODO we should have a better recommendation for other platforms
@Stable
public interface CircuitSaver {
  /** Returns a saveable representation of [value], or null to skip persisting it. */
  public fun save(value: CircuitSaveable): Any?

  /** Restores a [Screen] previously returned by [save], or null if it cannot be restored. */
  public fun <T : CircuitSaveable> restore(saved: Any): T?

  public companion object {
    /**
     * A [CircuitSaver] that persists nothing. Stacks saved with this restore to their initial
     * state.
     */
    public val NoOp: CircuitSaver = NoOpCircuitSaver
  }
}

/**
 * The default [CircuitSaver] for the current platform.
 *
 * On Android, screens and results pass through unchanged and are persisted via their `Parcelable`
 * implementations. Other platforms hold saved state in memory, so values also pass through
 * unchanged.
 */
public expect val DefaultCircuitSaver: CircuitSaver

/**
 * The [CircuitSaver] used by Circuit's saveable back stack implementations when one is not passed
 * explicitly. Defaults to [DefaultCircuitSaver].
 *
 * Provide this at the app root (see [ProvideCircuitSaver]) so it reaches back stacks created
 * anywhere in the composition, including ones created outside `CircuitCompositionLocals`.
 */
public val LocalCircuitSaver: ProvidableCompositionLocal<CircuitSaver> = staticCompositionLocalOf {
  DefaultCircuitSaver
}

/** Provides [circuitSaver] as [LocalCircuitSaver] to [content]. */
@Composable
public fun ProvideCircuitSaver(circuitSaver: CircuitSaver, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalCircuitSaver provides circuitSaver, content = content)
}

/** Passes values through unchanged. */
internal object PassThroughCircuitSaver : CircuitSaver {
  override fun save(value: CircuitSaveable): Any = value

  override fun <T : CircuitSaveable> restore(saved: Any): T? {
    @Suppress("UNCHECKED_CAST")
    return saved as? T
  }
}

private object NoOpCircuitSaver : CircuitSaver {
  override fun save(value: CircuitSaveable): Any? = null

  override fun <T : CircuitSaveable> restore(saved: Any): T? = null
}

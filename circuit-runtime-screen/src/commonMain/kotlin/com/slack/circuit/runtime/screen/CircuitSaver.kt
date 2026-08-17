// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.runtime.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Converts [Screen]s and [PopResult]s to and from representations that can be stored in a Compose
 * `SaveableStateRegistry`.
 *
 * Circuit's saveable back and nav stack implementations use this to persist navigation state.
 * Implementations choose the stored representation. Returned values must be supported by the
 * platform's `SaveableStateRegistry`.
 */
@Stable
public abstract class CircuitSaver protected constructor() {
  /** Returns a saveable representation of [value], or null to skip persisting it. */
  public abstract fun save(value: CircuitSaveable): Any?

  /**
   * Restores a [CircuitSaveable] previously returned by [save], or null if it cannot be restored.
   */
  protected abstract fun restore(saved: Any): CircuitSaveable?

  public companion object {
    @PublishedApi
    internal fun restoreForInline(
      saver: CircuitSaver,
      saved: Any,
    ): CircuitSaveable? = saver.restore(saved)

    /**
     * A [CircuitSaver] that persists nothing. Stacks saved with this restore to their initial
     * state.
     */
    public val NoOp: CircuitSaver = NoOpCircuitSaver
  }
}

/**
 * Restores [saved] as a [T].
 *
 * If this saver returns null, [onAbsent] is invoked and this returns null. If it restores a
 * [CircuitSaveable] that is not a [T], [onTypeMismatch] is invoked and this returns null if the
 * callback completes normally. By default, [onAbsent] does nothing and [onTypeMismatch] throws.
 */
public inline fun <reified T : Screen> CircuitSaver.restoreScreen(
  saved: Any,
  onAbsent: () -> Unit = {},
  onTypeMismatch: (CircuitSaveable) -> Unit = {
    error("Expected ${T::class}, but CircuitSaver restored ${it::class}.")
  },
): T? {
  val restored = CircuitSaver.restoreForInline(this, saved)
  if (restored == null) {
    onAbsent()
    return null
  }
  if (restored !is T) {
    onTypeMismatch(restored)
    return null
  }
  return restored
}

/**
 * Restores [saved] as a [T].
 *
 * If this saver returns null, [onAbsent] is invoked and this returns null. If it restores a
 * [CircuitSaveable] that is not a [T], [onTypeMismatch] is invoked and this returns null if the
 * callback completes normally. By default, [onAbsent] does nothing and [onTypeMismatch] throws.
 */
public inline fun <reified T : PopResult> CircuitSaver.restorePopResult(
  saved: Any,
  onAbsent: () -> Unit = {},
  onTypeMismatch: (CircuitSaveable) -> Unit = {
    error("Expected ${T::class}, but CircuitSaver restored ${it::class}.")
  },
): T? {
  val restored = CircuitSaver.restoreForInline(this, saved)
  if (restored == null) {
    onAbsent()
    return null
  }
  if (restored !is T) {
    onTypeMismatch(restored)
    return null
  }
  return restored
}

/**
 * The default [CircuitSaver] for the current platform.
 *
 * On Android, `Parcelable` screens and results pass through unchanged and other values are omitted.
 * Other platforms pass values through unchanged.
 */
public expect val DefaultCircuitSaver: CircuitSaver

/**
 * The [CircuitSaver] used by Circuit's saveable back stack implementations when one is not passed
 * explicitly. Defaults to [DefaultCircuitSaver]. Provide this at the app root (see
 * [ProvideCircuitSaver]) so it reaches back stacks created anywhere in the composition, including
 * ones created outside `CircuitCompositionLocals`.
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
internal object PassThroughCircuitSaver : CircuitSaver() {
  override fun save(value: CircuitSaveable): Any = value

  override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
}

private object NoOpCircuitSaver : CircuitSaver() {
  override fun save(value: CircuitSaveable): Any? = null

  override fun restore(saved: Any): CircuitSaveable? = null
}

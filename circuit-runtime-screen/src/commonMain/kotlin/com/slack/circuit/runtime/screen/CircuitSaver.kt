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
 *
 * Savers fail fast at save time when handed a value they do not support, since the developer can
 * fix the value's type immediately. Restoration degrades instead. Saved data can come from an older
 * app version, so unrestorable values restore as null and stacks fall back to their initial state.
 */
@Stable
public abstract class CircuitSaver protected constructor() {
  /**
   * Returns true when this saver handles saving [value].
   *
   * Composite savers select the first saver whose [canSave] returns true. That saver's [save]
   * result is final, even when it returns null or throws. The default is false so existing savers
   * do not claim values when used in a composite unless they opt in.
   */
  protected open fun canSave(value: CircuitSaveable): Boolean = false

  /** Returns a saveable representation of [value], or null to skip persisting it. */
  public abstract fun save(value: CircuitSaveable): Any?

  /**
   * Returns true when this saver handles restoring [saved].
   *
   * Composite savers select the first saver whose [canRestore] returns true. That saver's
   * restoration result is final, even when it returns null or throws. The default is false so
   * existing savers do not claim saved values when used in a composite unless they opt in.
   */
  protected open fun canRestore(saved: Any): Boolean = false

  /**
   * Restores a [CircuitSaveable] previously returned by [save], or null if it cannot be restored.
   */
  protected abstract fun restore(saved: Any): CircuitSaveable?

  public companion object {
    internal fun canSaveForComposite(
      saver: CircuitSaver,
      value: CircuitSaveable,
    ): Boolean = saver.canSave(value)

    internal fun canRestoreForComposite(saver: CircuitSaver, saved: Any): Boolean =
      saver.canRestore(saved)

    @PublishedApi
    internal fun restoreForInline(
      saver: CircuitSaver,
      saved: Any,
    ): CircuitSaveable? = saver.restore(saved)

    /**
     * A [CircuitSaver] that persists nothing. Stacks saved with this restore to their initial
     * state.
     */
    public val NoOp: CircuitSaver = DroppingCircuitSaver {}

    /**
     * Returns a [CircuitSaver] that claims every value and persists none of them, reporting each
     * dropped value to [onDropped].
     *
     * Append this to a composite to drop values that no earlier saver supports instead of failing
     * the save. Use [NoOp] when the drops do not need to be observed.
     */
    public fun Dropping(onDropped: (CircuitSaveable) -> Unit): CircuitSaver =
      DroppingCircuitSaver(onDropped)
  }
}

/**
 * Returns a saver that tries this saver and then [other].
 *
 * The first saver that claims a value is the only saver invoked. Saving throws if neither saver
 * claims the value, while restoration returns null. Append [CircuitSaver.NoOp] to silently drop
 * values that no earlier saver supports, or use [CircuitSaver.Dropping] to observe those drops.
 */
public operator fun CircuitSaver.plus(other: CircuitSaver): CircuitSaver =
  CompositeCircuitSaver(delegates + other.delegates)

private val CircuitSaver.delegates: List<CircuitSaver>
  get() =
    if (this is CompositeCircuitSaver) {
      delegates
    } else {
      listOf(this)
    }

private class CompositeCircuitSaver(val delegates: List<CircuitSaver>) : CircuitSaver() {
  override fun save(value: CircuitSaveable): Any? {
    val delegate =
      delegates.firstOrNull { canSaveForComposite(it, value) }
        ?: throw IllegalArgumentException(
          "No CircuitSaver in this composite can save ${value::class}. " +
            "Add a saver that supports this type, append CircuitSaver.NoOp to drop it, or " +
            "append CircuitSaver.Dropping { ... } to observe the drop."
        )
    return delegate.save(value)
  }

  protected override fun canSave(value: CircuitSaveable): Boolean = delegates.any {
    canSaveForComposite(it, value)
  }

  protected override fun canRestore(saved: Any): Boolean = delegates.any {
    canRestoreForComposite(it, saved)
  }

  protected override fun restore(saved: Any): CircuitSaveable? =
    delegates.firstOrNull { canRestoreForComposite(it, saved) }?.let { restoreForInline(it, saved) }
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
 * The default [CircuitSaver] used by Circuit's saveable back stacks and result handlers.
 *
 * `CircuitCompositionLocals` provides this for normal app usage. Use [ProvideCircuitSaver] in
 * another composition scope, or pass a saver directly to the remember API.
 */
public val LocalCircuitSaver: ProvidableCompositionLocal<CircuitSaver> = staticCompositionLocalOf {
  error(
    "No CircuitSaver provided. Wrap this content in CircuitCompositionLocals or " +
      "ProvideCircuitSaver, or pass a CircuitSaver directly to the remember API."
  )
}

private val LocalProvidedCircuitSaver = staticCompositionLocalOf<CircuitSaver?> { null }

/** Returns the saver installed by [ProvideCircuitSaver], or null when none is installed. */
@InternalCircuitSaverApi
@Composable
public fun currentProvidedCircuitSaverOrNull(): CircuitSaver? = LocalProvidedCircuitSaver.current

/** Provides [circuitSaver] as [LocalCircuitSaver] to [content]. */
@Composable
public fun ProvideCircuitSaver(circuitSaver: CircuitSaver, content: @Composable () -> Unit) {
  CompositionLocalProvider(
    LocalCircuitSaver provides circuitSaver,
    LocalProvidedCircuitSaver provides circuitSaver,
    content = content,
  )
}

private class DroppingCircuitSaver(private val onDropped: (CircuitSaveable) -> Unit) :
  CircuitSaver() {
  protected override fun canSave(value: CircuitSaveable): Boolean = true

  override fun save(value: CircuitSaveable): Any? {
    onDropped(value)
    return null
  }

  protected override fun canRestore(saved: Any): Boolean = true

  protected override fun restore(saved: Any): CircuitSaveable? = null
}

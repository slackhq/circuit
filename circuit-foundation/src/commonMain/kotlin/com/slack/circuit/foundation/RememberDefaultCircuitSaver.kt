// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver

/**
 * Returns a [CircuitSaver] that captures the nearest [LocalSaveableStateRegistry] and passes values
 * through when that registry accepts them.
 *
 * The captured registry is used only to decide which values this saver handles. `rememberSaveable`
 * still registers a stack with the registry at the stack's call site. Saveable-state holder child
 * registries delegate this check to their parent. If a custom nested registry accepts different
 * values, create and pass a saver from that registry's scope.
 *
 * [CircuitCompositionLocals] uses this automatically when no saver is configured on [Circuit] or
 * inherited from an outer `ProvideCircuitSaver`.
 *
 * Saving throws when there is no registry or the registry rejects a value. Append
 * [CircuitSaver.NoOp] or [CircuitSaver.Dropping] to explicitly drop unsupported values:
 * ```kotlin
 * val defaultSaver = rememberDefaultCircuitSaver()
 * remember(defaultSaver) { defaultSaver + CircuitSaver.NoOp }
 * ```
 */
@Composable
public fun rememberDefaultCircuitSaver(): CircuitSaver {
  val registry = LocalSaveableStateRegistry.current
  return remember(registry) { SaveableStateRegistryCircuitSaver(registry) }
}

private class SaveableStateRegistryCircuitSaver(private val registry: SaveableStateRegistry?) :
  CircuitSaver() {
  override fun canSave(value: CircuitSaveable): Boolean = registry?.canBeSaved(value) == true

  override fun save(value: CircuitSaveable): Any? {
    if (canSave(value)) return value
    throw IllegalArgumentException(
      "The current SaveableStateRegistry cannot save ${value::class}. Make the value saveable " +
        "(like Parcelable on Android), combine this saver with a serializing CircuitSaver, or " +
        "append CircuitSaver.NoOp or CircuitSaver.Dropping { ... } to drop unsupported values."
    )
  }

  protected override fun canRestore(saved: Any): Boolean = saved is CircuitSaveable

  protected override fun restore(saved: Any): CircuitSaveable? = saved as? CircuitSaveable
}

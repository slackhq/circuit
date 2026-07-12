// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.SavedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException

/**
 * Returns a [CircuitSaver] that persists [CircuitSaveable] types with kotlinx-serialization,
 * encoding them to `SavedState` via `androidx.savedstate`.
 *
 * In 0.35, Android [Screen] and [PopResult] implementations must still be `Parcelable`, even though
 * this saver stores `SavedState` rather than the Parcelable value. That Android supertype
 * requirement will be removed in a future release.
 *
 * Screens and results must be `@Serializable` and registered for polymorphic serialization against
 * the [CircuitSaveable] base class in [configuration]'s `serializersModule`:
 * ```
 * val saver = SerializableCircuitSaver(
 *   SavedStateConfiguration {
 *     serializersModule = SerializersModule {
 *       polymorphic(CircuitSaveable::class) {
 *         subclass(HomeScreen::class)
 *         subclass(DetailScreen::class)
 *         subclass(DetailResult::class)
 *       }
 *     }
 *   }
 * )
 * ```
 *
 * Saving an unregistered type fails with a descriptive error. Restoring an unregistered type, such
 * as after an app update removed a screen, drops that record instead of failing. Pass
 * [onRestoreError] to observe dropped records, such as for logging.
 *
 * On JVM and Android, `ReflectiveSerializableCircuitSaver` from the `circuit-serialization-reflect`
 * artifact can be used instead to avoid the registration requirement.
 */
public fun SerializableCircuitSaver(
  configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
  onRestoreError: (Throwable) -> Unit = {},
): CircuitSaver = SavedStateCircuitSaver(configuration, onRestoreError)

private class SavedStateCircuitSaver(
  private val configuration: SavedStateConfiguration,
  private val onRestoreError: (Throwable) -> Unit,
) : CircuitSaver() {
  private val circuitSaveableSerializer = PolymorphicSerializer(CircuitSaveable::class)

  override fun save(value: CircuitSaveable): Any {
    return encode(circuitSaveableSerializer, value)
  }

  protected override fun restore(saved: Any): CircuitSaveable? =
    decode(circuitSaveableSerializer, saved)

  private fun <T : Any> encode(serializer: KSerializer<T>, value: T): SavedState =
    try {
      encodeToSavedState(serializer, value, configuration)
    } catch (e: SerializationException) {
      throw IllegalArgumentException(
        "Unable to save ${value::class}. Ensure it is @Serializable and registered for " +
          "polymorphic serialization in the SavedStateConfiguration's serializersModule.",
        e,
      )
    }

  private fun <T : Any> decode(serializer: KSerializer<T>, saved: Any): T? {
    val savedState = saved as? SavedState ?: return null
    return try {
      decodeFromSavedState(serializer, savedState, configuration)
    } catch (e: SerializationException) {
      onRestoreError(e)
      null
    } catch (e: IllegalArgumentException) {
      onRestoreError(e)
      null
    }
  }
}

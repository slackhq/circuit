// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.SavedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException

/**
 * Returns a [CircuitSaver] that persists [CircuitSaveable] types with kotlinx-serialization,
 * encoding them to `SavedState` via `androidx.savedstate`.
 *
 * Screens and results must be `@Serializable` and registered for polymorphic serialization in
 * [configuration]'s `serializersModule`:
 * ```
 * val saver = SerializableCircuitSaver(
 *   SavedStateConfiguration {
 *     serializersModule = SerializersModule {
 *       polymorphic(Screen::class) {
 *         subclass(HomeScreen::class)
 *         subclass(DetailScreen::class)
 *       }
 *       polymorphic(PopResult::class) { subclass(DetailResult::class) }
 *     }
 *   }
 * )
 * ```
 *
 * Saving an unregistered type fails with a descriptive error. Restoring an unregistered type, such
 * as after an app update removed a screen, drops that record instead of failing.
 *
 * On JVM and Android, `ReflectiveSerializableCircuitSaver` can be used instead to avoid the
 * registration requirement.
 */
public fun SerializableCircuitSaver(
  configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT
): CircuitSaver = SavedStateCircuitSaver(configuration)

private class SavedStateCircuitSaver(private val configuration: SavedStateConfiguration) :
  CircuitSaver {
  private val circuitSaveableSerializer = PolymorphicSerializer(CircuitSaveable::class)

  override fun save(value: CircuitSaveable): Any? {
    return encode(circuitSaveableSerializer, value)
  }

  override fun <T : CircuitSaveable> restore(saved: Any): T? {
    @Suppress("UNCHECKED_CAST")
    return decode(circuitSaveableSerializer, saved) as? T?
  }

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
      // TODO report
      null
    } catch (e: IllegalArgumentException) {
      // TODO report
      null
    }
  }
}

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
import kotlinx.serialization.modules.plus

/**
 * Returns a [CircuitSaver] that persists [CircuitSaveable] types with kotlinx serialization. It
 * encodes them to `SavedState` with `androidx.savedstate`.
 *
 * Screens and results must have a kotlinx serializer and be registered for polymorphic
 * serialization against the [CircuitSaveable] base class. Use [Serializable] for manual
 * registration or [CircuitSerializable] with Circuit code generation. Manual registrations belong
 * in [configuration]'s `serializersModule`. Registrations against [CircuitSaveable] are also used
 * for nested properties declared as [Screen] or [PopResult]:
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
 * Saving an unregistered type fails with a descriptive error. Restoring an unregistered type
 * returns null, allowing the navigation owner to drop that record. Pass [onRestoreError] to observe
 * restoration failures.
 *
 * On JVM and Android, `ReflectiveSerializableCircuitSaver` from the `circuit-serialization-reflect`
 * artifact can be used instead to avoid the registration requirement.
 */
public fun SerializableCircuitSaver(
  configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
  onRestoreError: (Throwable) -> Unit = {},
): CircuitSaver = SavedStateCircuitSaver(configuration.withCircuitSerializers(), onRestoreError)

/**
 * Returns a [CircuitSaver] that persists the screens and pop results supplied by [registrations].
 *
 * The registrations are added to [configuration]'s existing serializers module. The other
 * configuration options are preserved. Conflicting serializer registrations fail when the saver is
 * created.
 */
public fun SerializableCircuitSaver(
  registrations: Iterable<CircuitSerializerRegistration>,
  configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
  onRestoreError: (Throwable) -> Unit = {},
): CircuitSaver =
  SavedStateCircuitSaver(
    configuration.withCircuitSerializers(registrations),
    onRestoreError,
  )

private fun SavedStateConfiguration.withCircuitSerializers(
  registrations: Iterable<CircuitSerializerRegistration>? = null
): SavedStateConfiguration {
  val sourceModule =
    if (registrations == null) {
      serializersModule
    } else {
      serializersModule + circuitSerializersModule(registrations)
    }
  return SavedStateConfiguration(from = this) {
    serializersModule = sourceModule.withCircuitSaveableFallbacks()
  }
}

private class SavedStateCircuitSaver(
  private val configuration: SavedStateConfiguration,
  private val onRestoreError: (Throwable) -> Unit,
) : CircuitSaver() {
  private val circuitSaveableSerializer = PolymorphicSerializer(CircuitSaveable::class)

  override fun save(value: CircuitSaveable): Any {
    return encode(circuitSaveableSerializer, value)
  }

  protected override fun restore(saved: Any): CircuitSaveable? =
    saved as? CircuitSaveable ?: decode(circuitSaveableSerializer, saved)

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

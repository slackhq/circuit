// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization.reflect

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

/**
 * Returns a [CircuitSaver] that persists `@Serializable` [Screen]s and [PopResult]s with
 * kotlinx-serialization, resolving serializers reflectively from the saved class name.
 *
 * Unlike `SerializableCircuitSaver`, this requires no polymorphic registration in
 * [configuration]'s `serializersModule`. It relies on JVM reflection (`Class.forName`), so it is
 * only available on JVM and Android.
 *
 * ## R8/ProGuard
 *
 * This artifact embeds the keep rules that reflective lookup needs, so minified Android apps work
 * without additional configuration. The embedded rules keep the names of all [CircuitSaveable]
 * implementations and their generated serializers.
 *
 * Because restore matches on class names, renaming or moving a screen class invalidates its
 * previously saved records. Restoring a class that no longer resolves, such as after an app update
 * removed a screen, drops that record instead of failing. Pass [onRestoreError] to observe dropped
 * records, such as for logging.
 */
public fun ReflectiveSerializableCircuitSaver(
  configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
  onRestoreError: (Throwable) -> Unit = {},
): CircuitSaver = ReflectiveCircuitSaver(configuration, onRestoreError)

private const val TYPE_KEY = "type"
private const val VALUE_KEY = "value"

private class ReflectiveCircuitSaver(
  private val configuration: SavedStateConfiguration,
  private val onRestoreError: (Throwable) -> Unit,
) : CircuitSaver {

  override fun save(value: CircuitSaveable): Any? = encode(value)

  override fun <T : CircuitSaveable> restore(saved: Any): T? {
    @Suppress("UNCHECKED_CAST")
    return decode(saved) as? T
  }

  private fun encode(value: Any): SavedState {
    val serializer =
      try {
        serializer(value.javaClass)
      } catch (e: SerializationException) {
        throw IllegalArgumentException(
          "Unable to save ${value::class}. Ensure it is @Serializable.",
          e,
        )
      }
    val encoded = encodeToSavedState(serializer, value, configuration)
    return savedState {
      putString(TYPE_KEY, value.javaClass.name)
      putSavedState(VALUE_KEY, encoded)
    }
  }

  private fun decode(saved: Any): Any? {
    val savedState = saved as? SavedState ?: return null
    return try {
      val (className, value) = savedState.read { getString(TYPE_KEY) to getSavedState(VALUE_KEY) }
      val serializer = serializer(Class.forName(className))
      decodeFromSavedState(serializer, value, configuration)
    } catch (e: ClassNotFoundException) {
      onRestoreError(e)
      null
    } catch (e: SerializationException) {
      onRestoreError(e)
      null
    } catch (e: IllegalArgumentException) {
      onRestoreError(e)
      null
    } catch (e: IllegalStateException) {
      onRestoreError(e)
      null
    }
  }
}

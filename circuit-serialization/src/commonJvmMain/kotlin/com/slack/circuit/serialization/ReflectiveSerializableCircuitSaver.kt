// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.CircuitSaver
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

/**
 * Returns a [CircuitSaver] that persists `@Serializable` [Screen]s and [PopResult]s with
 * kotlinx-serialization, resolving serializers reflectively from the saved class name.
 *
 * Unlike [SerializableCircuitSaver], this requires no polymorphic registration in [configuration]'s
 * `serializersModule`. It relies on JVM reflection (`Class.forName`), so it is only available on
 * JVM and Android, and screen classes plus their generated serializers must be kept when minifying
 * with R8/ProGuard.
 *
 * Restoring a class that no longer resolves, such as after an app update removed a screen, drops
 * that record instead of failing.
 */
public fun ReflectiveSerializableCircuitSaver(
  configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT
): CircuitSaver = ReflectiveCircuitSaver(configuration)

private const val TYPE_KEY = "type"
private const val VALUE_KEY = "value"

private class ReflectiveCircuitSaver(private val configuration: SavedStateConfiguration) :
  CircuitSaver {

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
      // TODO better error
      null
    } catch (e: SerializationException) {
      // TODO better error
      null
    } catch (e: IllegalArgumentException) {
      // TODO better error
      null
    } catch (e: IllegalStateException) {
      // TODO better error
      null
    }
  }
}

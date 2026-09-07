// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization.reflect

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.plus
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import com.slack.circuit.serialization.SerializableCircuitSaver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable data class ReflectiveScreen(val value: String) : Screen

@Serializable data class ReflectivePopResult(val value: Int) : PopResult

@Serializable
@SerialName("registered-reflective-screen")
data class RegisteredReflectiveScreen(val value: String) : Screen

data class NotSerializableScreen(val value: String) : Screen

class ReflectiveSerializableCircuitSaverTest {

  private val saver = ReflectiveSerializableCircuitSaver()

  @Test
  fun screen_round_trip() {
    val screen = ReflectiveScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restoreScreen<ReflectiveScreen>(saved))
  }

  @Test
  fun pop_result_round_trip() {
    val result = ReflectivePopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertEquals(result, saver.restorePopResult<ReflectivePopResult>(saved))
  }

  @Test
  fun raw_values_restore_without_reporting_an_error() {
    var reported: Throwable? = null
    val reportingSaver = ReflectiveSerializableCircuitSaver(onRestoreError = { reported = it })
    val screen = ReflectiveScreen("legacy")
    val result = ReflectivePopResult(42)

    assertEquals(screen, reportingSaver.restoreScreen<ReflectiveScreen>(screen))
    assertEquals(result, reportingSaver.restorePopResult<ReflectivePopResult>(result))
    assertNull(reported)
  }

  @Test
  fun composite_prefers_registered_serialization() {
    val composite = registeredSaver() + ReflectiveSerializableCircuitSaver()
    val screen = RegisteredReflectiveScreen("registered")

    val saved = assertIs<SavedState>(composite.save(screen))

    assertEquals("registered-reflective-screen", saved.read { getString("type") })
    assertEquals(screen, composite.restoreScreen<RegisteredReflectiveScreen>(saved))
  }

  @Test
  fun composite_falls_through_to_reflective_serialization() {
    val composite = SerializableCircuitSaver() + ReflectiveSerializableCircuitSaver()
    val screen = ReflectiveScreen("reflective")

    val saved = assertIs<SavedState>(composite.save(screen))

    assertEquals(ReflectiveScreen::class.java.name, saved.read { getString("type") })
    assertEquals(screen, composite.restoreScreen<ReflectiveScreen>(saved))
  }

  @Test
  fun composite_falls_back_for_a_non_serializable_value() {
    val screen = NotSerializableScreen("fallback")
    val fallback = RecordingCircuitSaver(saveResult = "fallback", restoreResult = screen)
    val composite = saver + fallback

    assertEquals("fallback", composite.save(screen))
    assertEquals(listOf<CircuitSaveable>(screen), fallback.savedValues)
    assertEquals(screen, composite.restoreScreen<NotSerializableScreen>("fallback"))
    assertEquals(1, fallback.restoreCalls)
  }

  @Test
  fun screen_cannot_restore_as_pop_result() {
    val screen = ReflectiveScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertFailsWith<IllegalStateException> {
      saver.restorePopResult<ReflectivePopResult>(saved)
    }
    var mismatched: CircuitSaveable? = null
    assertNull(
      saver.restorePopResult<ReflectivePopResult>(
        saved,
        onTypeMismatch = { mismatched = it },
      )
    )
    assertEquals(screen, mismatched)
  }

  @Test
  fun pop_result_cannot_restore_as_screen() {
    val result = ReflectivePopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertFailsWith<IllegalStateException> { saver.restoreScreen<ReflectiveScreen>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restoreScreen<ReflectiveScreen>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(result, mismatched)
  }

  @Test
  fun save_non_serializable_screen_fails() {
    assertFailsWith<IllegalArgumentException> { saver.save(NotSerializableScreen("nope")) }
  }

  @Test
  fun restore_missing_class_returns_null() {
    val saved = savedState {
      putString("type", "com.example.DoesNotExist")
      putSavedState("value", savedState {})
    }
    assertNull(saver.restoreScreen<ReflectiveScreen>(saved))
  }

  @Test
  fun restore_error_reports_to_callback() {
    val saved = savedState {
      putString("type", "com.example.DoesNotExist")
      putSavedState("value", savedState {})
    }
    var reported: Throwable? = null
    val reportingSaver = ReflectiveSerializableCircuitSaver(onRestoreError = { reported = it })
    assertNull(reportingSaver.restoreScreen<ReflectiveScreen>(saved))
    assertNotNull(reported)
  }

  @Test
  fun composite_routes_a_reflective_restore_error_once() {
    val saved = savedState { putString("type", ReflectiveScreen::class.java.name) }
    val registeredErrors = mutableListOf<Throwable>()
    val reflectiveErrors = mutableListOf<Throwable>()
    val composite =
      SerializableCircuitSaver(onRestoreError = registeredErrors::add) +
        ReflectiveSerializableCircuitSaver(onRestoreError = reflectiveErrors::add)

    assertNull(composite.restoreScreen<ReflectiveScreen>(saved))
    assertEquals(0, registeredErrors.size)
    assertEquals(1, reflectiveErrors.size)
  }

  @Test
  fun composite_does_not_retry_a_registered_restore_error_reflectively() {
    val saved = savedState {
      putString("type", "registered-reflective-screen")
      putSavedState("value", savedState {})
    }
    val registeredErrors = mutableListOf<Throwable>()
    val reflectiveErrors = mutableListOf<Throwable>()
    val composite =
      registeredSaver(onRestoreError = registeredErrors::add) +
        ReflectiveSerializableCircuitSaver(onRestoreError = reflectiveErrors::add)

    assertNull(composite.restoreScreen<RegisteredReflectiveScreen>(saved))
    assertEquals(1, registeredErrors.size)
    assertEquals(0, reflectiveErrors.size)
  }

  @Test
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restoreScreen<ReflectiveScreen>("garbage"))
  }
}

private fun registeredSaver(onRestoreError: (Throwable) -> Unit = {}): CircuitSaver =
  SerializableCircuitSaver(
    configuration =
      SavedStateConfiguration {
        serializersModule = SerializersModule {
          polymorphic(CircuitSaveable::class) { subclass(RegisteredReflectiveScreen::class) }
        }
      },
    onRestoreError = onRestoreError,
  )

private class RecordingCircuitSaver(
  private val saveResult: Any? = null,
  private val restoreResult: CircuitSaveable? = null,
) : CircuitSaver() {
  val savedValues = mutableListOf<CircuitSaveable>()
  var restoreCalls = 0

  override fun save(value: CircuitSaveable): Any? {
    savedValues += value
    return saveResult
  }

  override fun canSave(value: CircuitSaveable): Boolean = true

  override fun canRestore(saved: Any): Boolean = true

  protected override fun restore(saved: Any): CircuitSaveable? {
    restoreCalls++
    return restoreResult
  }
}

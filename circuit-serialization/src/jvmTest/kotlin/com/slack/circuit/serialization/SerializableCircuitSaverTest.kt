// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable data class StringScreen(val value: String) : Screen

@Serializable data object ObjectScreen : Screen

@Serializable data class IntPopResult(val value: Int) : PopResult

@Serializable data class StringPopResult(val value: String) : PopResult

data class UnregisteredScreen(val value: String) : Screen

class SerializableCircuitSaverTest {

  private val saver =
    SerializableCircuitSaver(
      SavedStateConfiguration {
        serializersModule = SerializersModule {
          polymorphic(CircuitSaveable::class) {
            subclass(StringScreen::class)
            subclass(ObjectScreen::class)
            subclass(IntPopResult::class)
            subclass(StringPopResult::class)
          }
        }
      }
    )

  @Test
  fun screen_round_trip() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restoreScreen<StringScreen>(saved))
  }

  @Test
  fun object_screen_round_trip() {
    val saved = assertNotNull(saver.save(ObjectScreen))
    assertEquals(ObjectScreen, saver.restoreScreen<ObjectScreen>(saved))
  }

  @Test
  fun pop_result_round_trip() {
    val result = IntPopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertEquals(result, saver.restorePopResult<IntPopResult>(saved))
  }

  @Test
  fun raw_values_restore_without_reporting_an_error() {
    var reported: Throwable? = null
    val reportingSaver = SerializableCircuitSaver(onRestoreError = { reported = it })
    val screen = StringScreen("legacy")
    val result = IntPopResult(42)

    assertEquals(screen, reportingSaver.restoreScreen<StringScreen>(screen))
    assertEquals(result, reportingSaver.restorePopResult<IntPopResult>(result))
    assertNull(reported)
  }

  @Test
  fun screen_cannot_restore_as_pop_result() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertFailsWith<IllegalStateException> { saver.restorePopResult<IntPopResult>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(
      saver.restorePopResult<IntPopResult>(saved, onTypeMismatch = { mismatched = it })
    )
    assertEquals(screen, mismatched)
  }

  @Test
  fun pop_result_cannot_restore_as_screen() {
    val result = IntPopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertFailsWith<IllegalStateException> { saver.restoreScreen<StringScreen>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restoreScreen<StringScreen>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(result, mismatched)
  }

  @Test
  fun screen_subtype_mismatch_fails_by_default() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertFailsWith<IllegalStateException> { saver.restoreScreen<ObjectScreen>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(saver.restoreScreen<ObjectScreen>(saved, onTypeMismatch = { mismatched = it }))
    assertEquals(screen, mismatched)
  }

  @Test
  fun pop_result_subtype_mismatch_fails_by_default() {
    val result = StringPopResult("hello")
    val saved = assertNotNull(saver.save(result))
    assertFailsWith<IllegalStateException> { saver.restorePopResult<IntPopResult>(saved) }
    var mismatched: CircuitSaveable? = null
    assertNull(
      saver.restorePopResult<IntPopResult>(saved, onTypeMismatch = { mismatched = it })
    )
    assertEquals(result, mismatched)
  }

  @Test
  fun save_unregistered_screen_fails() {
    assertFailsWith<IllegalArgumentException> { saver.save(UnregisteredScreen("nope")) }
  }

  @Test
  fun restore_unregistered_screen_returns_null() {
    val saved = assertNotNull(saver.save(StringScreen("hello")))
    val unconfiguredSaver = SerializableCircuitSaver()
    var absentHandled = false
    assertNull(
      unconfiguredSaver.restoreScreen<StringScreen>(
        saved,
        onAbsent = { absentHandled = true },
      )
    )
    assertTrue(absentHandled)
  }

  @Test
  fun restore_error_reports_to_callback() {
    val saved = assertNotNull(saver.save(StringScreen("hello")))
    var reported: Throwable? = null
    val unconfiguredSaver = SerializableCircuitSaver(onRestoreError = { reported = it })
    assertNull(unconfiguredSaver.restoreScreen<StringScreen>(saved))
    assertNotNull(reported)
  }

  @Test
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restoreScreen<StringScreen>("garbage"))
  }
}

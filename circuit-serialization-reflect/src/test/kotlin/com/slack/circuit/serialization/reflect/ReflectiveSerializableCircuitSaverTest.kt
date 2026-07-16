// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization.reflect

import androidx.savedstate.savedState
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
import kotlinx.serialization.Serializable

@Serializable data class ReflectiveScreen(val value: String) : Screen

@Serializable data class ReflectivePopResult(val value: Int) : PopResult

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
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restoreScreen<ReflectiveScreen>("garbage"))
  }
}

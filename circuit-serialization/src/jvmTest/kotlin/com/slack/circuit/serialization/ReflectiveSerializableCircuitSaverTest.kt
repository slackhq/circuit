// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.savedState
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
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
    val saved = assertNotNull(saver.saveScreen(screen))
    assertEquals(screen, saver.restoreScreen(saved))
  }

  @Test
  fun pop_result_round_trip() {
    val result = ReflectivePopResult(42)
    val saved = assertNotNull(saver.savePopResult(result))
    assertEquals(result, saver.restorePopResult(saved))
  }

  @Test
  fun save_non_serializable_screen_fails() {
    assertFailsWith<IllegalArgumentException> { saver.saveScreen(NotSerializableScreen("nope")) }
  }

  @Test
  fun restore_missing_class_returns_null() {
    val saved = savedState {
      putString("type", "com.example.DoesNotExist")
      putSavedState("value", savedState {})
    }
    assertNull(saver.restoreScreen(saved))
  }

  @Test
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restoreScreen("garbage"))
  }
}

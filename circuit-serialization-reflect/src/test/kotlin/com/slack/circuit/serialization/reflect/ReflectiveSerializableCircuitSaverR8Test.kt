// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization.reflect

import androidx.savedstate.SavedState
import androidx.savedstate.read
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.Serializable

@Serializable data class R8DataScreen(val value: String) : Screen

@Serializable data object R8ObjectScreen : Screen

@Serializable data class R8DataPopResult(val value: Int) : PopResult

class ReflectiveSerializableCircuitSaverR8Test {

  private val saver = ReflectiveSerializableCircuitSaver()

  @Test
  fun data_screen_keeps_name_and_round_trips() {
    val screen = R8DataScreen("hello")
    val saved = assertNotNull(saver.save(screen)) as SavedState

    assertEquals(expectedBinaryName("Data", "Screen"), saved.read { getString("type") })
    assertEquals(screen, saver.restoreScreen<R8DataScreen>(saved))
  }

  @Test
  fun object_screen_keeps_name_and_round_trips() {
    val saved = assertNotNull(saver.save(R8ObjectScreen)) as SavedState

    assertEquals(expectedBinaryName("Object", "Screen"), saved.read { getString("type") })
    assertEquals(R8ObjectScreen, saver.restoreScreen<R8ObjectScreen>(saved))
  }

  @Test
  fun pop_result_keeps_name_and_round_trips() {
    val result = R8DataPopResult(42)
    val saved = assertNotNull(saver.save(result)) as SavedState

    assertEquals(expectedBinaryName("Data", "Pop", "Result"), saved.read { getString("type") })
    assertEquals(result, saver.restorePopResult<R8DataPopResult>(saved))
  }

  private fun expectedBinaryName(vararg simpleNameParts: String): String {
    val packageName =
      arrayOf("com", "slack", "circuit", "serialization", "reflect").joinToString(".")
    return packageName + "." + "R8" + simpleNameParts.joinToString("")
  }
}

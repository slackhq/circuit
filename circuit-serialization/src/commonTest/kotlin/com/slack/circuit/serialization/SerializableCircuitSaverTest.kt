// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable data class StringScreen(val value: String) : Screen

@Serializable data object ObjectScreen : Screen

@Serializable data class IntPopResult(val value: Int) : PopResult

data class UnregisteredScreen(val value: String) : Screen

class SerializableCircuitSaverTest {

  private val saver =
    SerializableCircuitSaver(
      SavedStateConfiguration {
        serializersModule = SerializersModule {
          polymorphic(Screen::class) {
            subclass(StringScreen::class)
            subclass(ObjectScreen::class)
          }
          polymorphic(PopResult::class) { subclass(IntPopResult::class) }
        }
      }
    )

  @Test
  fun screen_round_trip() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.saveScreen(screen))
    assertEquals(screen, saver.restoreScreen(saved))
  }

  @Test
  fun object_screen_round_trip() {
    val saved = assertNotNull(saver.saveScreen(ObjectScreen))
    assertEquals(ObjectScreen, saver.restoreScreen(saved))
  }

  @Test
  fun pop_result_round_trip() {
    val result = IntPopResult(42)
    val saved = assertNotNull(saver.savePopResult(result))
    assertEquals(result, saver.restorePopResult(saved))
  }

  @Test
  fun save_unregistered_screen_fails() {
    assertFailsWith<IllegalArgumentException> { saver.saveScreen(UnregisteredScreen("nope")) }
  }

  @Test
  fun restore_unregistered_screen_returns_null() {
    val saved = assertNotNull(saver.saveScreen(StringScreen("hello")))
    val unconfiguredSaver = SerializableCircuitSaver()
    assertNull(unconfiguredSaver.restoreScreen(saved))
  }

  @Test
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restoreScreen("garbage"))
  }
}

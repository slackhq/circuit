// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.screen.CircuitSaveable
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

@Parcelize @Serializable data class StringScreen(val value: String) : Screen

@Parcelize @Serializable data object ObjectScreen : Screen

@Parcelize @Serializable data class IntPopResult(val value: Int) : PopResult

@Parcelize data class UnregisteredScreen(val value: String) : Screen

@RunWith(SavedStateTestRunner::class)
class SerializableCircuitSaverTest {

  private val saver =
    SerializableCircuitSaver(
      SavedStateConfiguration {
        serializersModule = SerializersModule {
          polymorphic(CircuitSaveable::class) {
            subclass(StringScreen::class)
            subclass(ObjectScreen::class)
            subclass(IntPopResult::class)
          }
        }
      }
    )

  @Test
  fun screen_round_trip() {
    val screen = StringScreen("hello")
    val saved = assertNotNull(saver.save(screen))
    assertEquals(screen, saver.restore<Screen>(saved))
  }

  @Test
  fun object_screen_round_trip() {
    val saved = assertNotNull(saver.save(ObjectScreen))
    assertEquals(ObjectScreen, saver.restore<Screen>(saved))
  }

  @Test
  fun pop_result_round_trip() {
    val result = IntPopResult(42)
    val saved = assertNotNull(saver.save(result))
    assertEquals(result, saver.restore<PopResult>(saved))
  }

  @Test
  fun save_unregistered_screen_fails() {
    assertFailsWith<IllegalArgumentException> { saver.save(UnregisteredScreen("nope")) }
  }

  @Test
  fun restore_unregistered_screen_returns_null() {
    val saved = assertNotNull(saver.save(StringScreen("hello")))
    val unconfiguredSaver = SerializableCircuitSaver()
    assertNull(unconfiguredSaver.restore<Screen>(saved))
  }

  @Test
  fun restore_unexpected_value_returns_null() {
    assertNull(saver.restore<Screen>("garbage"))
  }
}

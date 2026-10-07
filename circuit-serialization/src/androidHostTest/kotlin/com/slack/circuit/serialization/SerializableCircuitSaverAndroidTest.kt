// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restorePopResult
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SerializableCircuitSaverAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val saver =
    SerializableCircuitSaver(
      SavedStateConfiguration {
        serializersModule = SerializersModule {
          polymorphic(CircuitSaveable::class) {
            subclass(SerializableScreen::class)
            subclass(SerializablePopResult::class)
          }
        }
      }
    )

  @Test
  fun saveUsesSavedStateForPlainScreenAndPopResult() {
    val screen = SerializableScreen("value")
    val result = SerializablePopResult(42)

    val savedScreen = saver.save(screen)
    val savedResult = saver.save(result)

    assertIs<Bundle>(savedScreen)
    assertIs<Bundle>(savedResult)
    assertNotSame<Any>(screen, savedScreen)
    assertNotSame<Any>(result, savedResult)
    assertEquals(screen, saver.restoreScreen<SerializableScreen>(savedScreen))
    assertEquals(result, saver.restorePopResult<SerializablePopResult>(savedResult))
  }

  @Test
  fun rememberSaveableRestoresFromSerializedSavedState() {
    val restorationTester = StateRestorationTester(composeTestRule)
    val initialScreen = SerializableScreen("restored")
    var initialCalls = 0
    lateinit var screen: SerializableScreen
    val screenSaver =
      Saver<SerializableScreen, Any>(
        save = { saver.save(it) },
        restore = { saver.restoreScreen<SerializableScreen>(it) },
      )
    restorationTester.setContent {
      screen =
        rememberSaveable(saver = screenSaver) {
          initialCalls++
          initialScreen
        }
    }

    restorationTester.emulateSavedInstanceStateRestore()

    assertEquals(1, initialCalls)
    assertEquals(initialScreen, screen)
    assertNotSame(initialScreen, screen)
  }
}

@Serializable private data class SerializableScreen(val value: String) : Screen

@Serializable private data class SerializablePopResult(val value: Int) : PopResult

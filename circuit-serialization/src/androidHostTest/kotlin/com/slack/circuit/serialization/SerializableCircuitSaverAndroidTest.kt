// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.savedstate.serialization.SavedStateConfiguration
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.Screen
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
            subclass(SerializableParcelableScreen::class)
          }
        }
      }
    )

  @Test
  fun saveUsesSavedStateInsteadOfPassingThroughParcelable() {
    val screen = SerializableParcelableScreen("value")

    val saved = saver.save(screen)

    assertIs<Bundle>(saved)
    assertNotSame<Any>(screen, saved)
    assertEquals(screen, saver.restoreScreen<SerializableParcelableScreen>(saved))
  }

  @Test
  fun rememberSaveableRestoresFromSerializedSavedState() {
    val restorationTester = StateRestorationTester(composeTestRule)
    val initialScreen = SerializableParcelableScreen("restored")
    var initialCalls = 0
    lateinit var screen: SerializableParcelableScreen
    val screenSaver =
      Saver<SerializableParcelableScreen, Any>(
        save = { saver.save(it) },
        restore = { saver.restoreScreen<SerializableParcelableScreen>(it) },
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

@Serializable
private data class SerializableParcelableScreen(val value: String) : Screen {
  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(value)
  }

  companion object CREATOR : Parcelable.Creator<SerializableParcelableScreen> {
    override fun createFromParcel(source: Parcel): SerializableParcelableScreen =
      SerializableParcelableScreen(checkNotNull(source.readString()))

    override fun newArray(size: Int): Array<SerializableParcelableScreen?> = arrayOfNulls(size)
  }
}

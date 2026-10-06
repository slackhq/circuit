// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.serialization.reflect

import android.os.Bundle
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.restoreScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.Serializable
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReflectiveSerializableCircuitSaverAndroidTest {

  @Test
  fun serializableScreenSavesAndRestoresWithAndroidSavedState() {
    val saver = ReflectiveSerializableCircuitSaver()

    val saved = assertIs<Bundle>(saver.save(AndroidScreen))

    assertEquals(AndroidScreen, saver.restoreScreen<AndroidScreen>(saved))
  }
}

@Serializable data object AndroidScreen : Screen

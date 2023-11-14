// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import coil.annotation.ExperimentalCoilApi
import com.android.ide.common.rendering.api.SessionParams
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.ui.StarTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalCoilApi
class PetListPaparazziSnapshotTest {

  companion object {
    val ANIMAL =
      PetListAnimal(
        id = 1L,
        name = "Baxter",
        imageUrl = "http://some.url",
        breed = "Australian Terrier",
        gender = Gender.MALE,
        size = Size.SMALL,
        age = "12"
      )
  }

  @get:Rule
  val paparazzi =
    Paparazzi(
      environment =
  detectEnvironment()
    .copy(
      platformDir = "${androidHome()}/platforms/android-33",
      compileSdkVersion = 33
    ),
      renderingMode = SessionParams.RenderingMode.SHRINK,
    )

  @get:Rule val coilRule = CoilRule(contextProvider = paparazzi::context)

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  @Test
  fun example() {
    val animals = persistentListOf(ANIMAL)
    paparazzi.snapshot {
      StarTheme {
        PetList(PetListScreen.State.Success(animals, isRefreshing = false))
      }
    }
  }
}

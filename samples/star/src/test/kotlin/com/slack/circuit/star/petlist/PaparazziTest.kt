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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalCoilApi
@RunWith(Parameterized::class)
class PetListPaparazziSnapshotTest(private val useDarkMode: Boolean) {

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

    const val SNAPSHOT_TAG = "snapshot_tag"

    @JvmStatic
    @Parameterized.Parameters(name = "darkMode={0}")
    fun data() = listOf(true, false)
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
//      options = RoborazziRule.Options(outputDirectoryPath = "src/test/snapshots/images")
    )

  @get:Rule val coilRule = CoilRule(contextProvider = composeTestRule::activity)

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  private fun snapshot(body: @Composable (Modifier) -> Unit) {
    paparazzi.snapshot {
      StarTheme(useDarkTheme = useDarkMode) { body(Modifier.testTag(SNAPSHOT_TAG)) }
    }
  }

  @Test
  fun petList_show_message_for_no_animals_state() = snapshot { modifier ->
    PetList(PetListScreen.State.NoAnimals(isRefreshing = false), modifier)
  }

  @Test
  fun petList_show_list_for_success_state() = snapshot { modifier ->
    val animals = persistentListOf(ANIMAL)
    PetList(PetListScreen.State.Success(animals, isRefreshing = false), modifier)
  }

  @Test
  fun petList_filtersSheet() = snapshot { modifier ->
    Surface(modifier) { UpdateFiltersSheet(initialFilters = Filters()) }
  }
}

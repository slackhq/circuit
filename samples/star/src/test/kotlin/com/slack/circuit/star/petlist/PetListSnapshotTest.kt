// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import coil.annotation.ExperimentalCoilApi
import com.android.ide.common.rendering.api.SessionParams
import com.slack.circuit.sample.coil.test.CoilRule
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
class PetListSnapshotTest(private val useDarkMode: Boolean) {

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

    @JvmStatic @Parameterized.Parameters(name = "darkMode={0}") fun data() = listOf(true, false)
  }

  @get:Rule
  val paparazzi =
    Paparazzi(
      deviceConfig = PIXEL_5,
      theme = "com.slack.circuit.star.ui.StarTheme",
      renderingMode = SessionParams.RenderingMode.SHRINK,
      showSystemUi = false,
      maxPercentDifference = 0.2,
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

  private fun snapshot(body: @Composable () -> Unit) {
    paparazzi.snapshot { StarTheme(useDarkTheme = useDarkMode) { body() } }
  }

  @Ignore(
    """
    Kinda pointless until we can set a virtual timer to progress the loading indicator to
    something useful.
    https://github.com/cashapp/paparazzi/issues/513
  """
  )
  @Test
  fun petList_show_progress_indicator_for_loading_state() = snapshot {
    PetList(PetListScreen.State.Loading)
  }

  @Test
  fun petList_show_message_for_no_animals_state() = snapshot {
    PetList(PetListScreen.State.NoAnimals(isRefreshing = false))
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = persistentListOf(ANIMAL)
    snapshot { PetList(PetListScreen.State.Success(animals, isRefreshing = false) {}) }
  }

  @Test fun petList_filtersSheet() = snapshot { PreviewUpdateFiltersSheet() }
}

/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.star.petlist

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import coil.Coil
import com.slack.circuit.star.ui.FakeImageLoader
import com.slack.circuit.star.ui.StarTheme
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

@RunWith(Parameterized::class)
class PetListSnapshotTest(private val useDarkMode: Boolean) {

  companion object {
    val ANIMAL =
      PetListAnimal(
        id = 1L,
        name = "Baxter",
        imageUrl = "http://some.url",
        breed = "Australian Terrier",
        gender = "male",
        size = "small",
        age = "12"
      )

    @JvmStatic @Parameterized.Parameters(name = "darkMode={0}") fun data() = listOf(true, false)
  }

  @get:Rule
  val paparazzi =
    Paparazzi(
      deviceConfig = PIXEL_5,
      theme = "com.slack.circuit.star.ui.StarTheme",
    )

  @Before
  fun setup() {
    val fakeImageLoader = FakeImageLoader()
    Coil.setImageLoader(fakeImageLoader)
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
  fun petList_show_progress_indicator_for_loading_state() {
    snapshot { PetList(PetListScreen.State.Loading) }
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    snapshot { PetList(PetListScreen.State.NoAnimals(isRefreshing = false)) }
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = listOf(ANIMAL)
    snapshot { PetList(PetListScreen.State.Success(animals, isRefreshing = false) {}) }
  }

  @Test
  fun petList_filtersSheet() {
    snapshot { PreviewUpdateFiltersSheet() }
  }
}

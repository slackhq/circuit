// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import coil.annotation.ExperimentalCoilApi
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.slack.circuit.foundation.PreviewSharedElementTransitionLayout
import com.slack.circuit.foundation.SharedElementTransitionLayout
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.star.db.Gender.MALE
import com.slack.circuit.star.db.Size.SMALL
import com.slack.circuit.star.petlist.PetListScreen.State.Loading
import com.slack.circuit.star.petlist.PetListScreen.State.NoAnimals
import com.slack.circuit.star.petlist.PetListScreen.State.Success
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
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ExperimentalCoilApi
@RunWith(ParameterizedRobolectricTestRunner::class)
class PetListSnapshotTest(private val useDarkMode: Boolean) {

  companion object {
    val ANIMAL =
      PetListAnimal(
        id = 1L,
        name = "Baxter",
        imageUrl = "http://some.url",
        breed = "Australian Terrier",
        gender = MALE,
        size = SMALL,
        age = "12",
      )

    const val SNAPSHOT_TAG = "snapshot_tag"

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "darkMode={0}")
    fun data() = listOf(true, false)
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val roborazziRule =
    RoborazziRule(
      composeRule = composeTestRule,
      captureRoot = composeTestRule.onRoot(),
      options = RoborazziRule.Options(outputDirectoryPath = "src/test/snapshots/images"),
    )

  @get:Rule val coilRule = CoilRule()

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  private fun snapshot(body: @Composable (Modifier) -> Unit) {
    composeTestRule.setContent {
      StarTheme(useDarkTheme = useDarkMode) { body(Modifier.testTag(SNAPSHOT_TAG)) }
    }
    composeTestRule.onNodeWithTag(SNAPSHOT_TAG).captureRoboImage()
  }

  @Ignore(
    """
    Kinda pointless until we can set a virtual timer to progress the loading indicator to
    something useful.
    https://github.com/cashapp/paparazzi/issues/513
  """
  )
  @Test
  fun petList_show_progress_indicator_for_loading_state() = snapshot { modifier ->
    PetList(Loading, modifier)
  }

  @Test
  fun petList_show_message_for_no_animals_state() = snapshot { modifier ->
    PetList(NoAnimals(isRefreshing = false), modifier)
  }

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Test
  fun petList_show_list_for_success_state() = snapshot { modifier ->
    PreviewSharedElementTransitionLayout {
      val animals = persistentListOf(ANIMAL)
      PetList(Success(animals, isRefreshing = false), modifier)
    }
  }

  @Test
  fun petList_filtersSheet() = snapshot { modifier ->
    Surface(modifier) { UpdateFiltersSheet(initialFilters = Filters()) }
  }
}

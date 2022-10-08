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
package com.slack.circuit.star.petdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.star.FakeImageLoader
import com.slack.circuit.star.R
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PetDetailTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    val fakeImageLoader =
      FakeImageLoader(InstrumentationRegistry.getInstrumentation().targetContext, R.drawable.dog2)
    Coil.setImageLoader(fakeImageLoader)
  }

  @Test
  fun petDetail_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { PetDetail(PetDetailScreen.State.Loading) }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petDetail_show_message_for_unknown_animal_state() {
    composeTestRule.run {
      setContent { PetDetail(PetDetailScreen.State.UnknownAnimal) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()

      onNodeWithTag(UNKNOWN_ANIMAL_TAG)
        .assertIsDisplayed()
        .assertTextEquals(activity.getString(R.string.unknown_animals))
    }
  }

  @Test
  fun petDetail_show_animal_for_success_state() {
    val success =
      PetDetailScreen.State.Success(
        url = "url",
        photoUrls = listOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier",
        tags = listOf("dog", "terrier", "male"),
      )

    var carouselScreen: PetPhotoCarouselScreen? = null
    val circuitConfig =
      CircuitConfig.Builder()
        .setOnUnavailableContent { screen ->
          carouselScreen = screen as PetPhotoCarouselScreen
          PetPhotoCarousel(PetPhotoCarouselScreen.State(screen))
        }
        .build()

    val expectedScreen =
      PetPhotoCarouselScreen(
        name = success.name,
        photoUrls = success.photoUrls,
        photoUrlMemoryCacheKey = null
      )

    composeTestRule.run {
      setContent { CircuitCompositionLocals(circuitConfig) { PetDetail(success) } }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()

      onNodeWithTag(CAROUSEL_TAG).assertIsDisplayed().performTouchInput { swipeUp() }
      onNodeWithText(success.name).assertIsDisplayed()
      onNodeWithText(success.description).assertIsDisplayed()

      assertThat(carouselScreen).run {
        isNotNull()
        isEqualTo(expectedScreen)
      }
    }
  }
}

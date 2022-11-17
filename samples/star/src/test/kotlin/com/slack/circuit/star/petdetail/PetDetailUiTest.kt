// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.CircuitCompositionLocals
import com.slack.circuit.CircuitConfig
import com.slack.circuit.star.R
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.star.ui.FakeImageLoader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PetDetailUiTest {
  @get:Rule val composeTestRule = createComposeRule()

  // TODO this seems like not the greatest test pattern, maybe something we can offer better
  //  solutions for via semantics.
  private var carouselScreen: PetPhotoCarouselScreen? = null
  private val circuitConfig =
    CircuitConfig.Builder()
      .setOnUnavailableContent { screen ->
        when (screen) {
          is PetPhotoCarouselScreen -> {
            PetPhotoCarousel(PetPhotoCarouselScreen.State(screen))
            carouselScreen = screen
          }
        }
      }
      .build()

  @Before
  fun setup() {
    val fakeImageLoader =
      FakeImageLoader(
        InstrumentationRegistry.getInstrumentation().targetContext.getDrawable(R.drawable.dog2)!!
      )
    Coil.setImageLoader(fakeImageLoader)
  }

  @Test
  fun petDetail_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuitConfig) { PetDetail(PetDetailScreen.State.Loading) }
      }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petDetail_show_message_for_unknown_animal_state() {
    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuitConfig) { PetDetail(PetDetailScreen.State.UnknownAnimal) }
      }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()

      onNodeWithTag(UNKNOWN_ANIMAL_TAG)
        .assertIsDisplayed()
        .assertTextEquals(
          InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.unknown_animals)
        )
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
        eventSink = {}
      )

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

  @Test
  fun petDetail_emits_event_when_tapping_on_full_bio_button() = runTest {
    val channel = Channel<Any>(1)

    val success =
      PetDetailScreen.State.Success(
        url = "url",
        photoUrls = listOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier",
        tags = listOf("dog", "terrier", "male"),
        eventSink = channel::trySend
      )

    val circuitConfig =
      CircuitConfig.Builder()
        .setOnUnavailableContent { screen ->
          PetPhotoCarousel(PetPhotoCarouselScreen.State(screen as PetPhotoCarouselScreen))
        }
        .build()

    composeTestRule.run {
      setContent { CircuitCompositionLocals(circuitConfig) { PetDetail(success) } }

      onNodeWithTag(CAROUSEL_TAG).assertIsDisplayed().performTouchInput { swipeUp() }
      onNodeWithTag(FULL_BIO_TAG, true).assertIsDisplayed().performClick()

      val event = channel.receive()
      assertThat(event).isEqualTo(PetDetailScreen.Event.ViewFullBio(success.url))
    }
  }
}

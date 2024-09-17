// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import coil3.test.FakeImage
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.petdetail.PetDetailScreen.Event
import com.slack.circuit.star.petdetail.PetDetailScreen.Event.ViewFullBio
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Success
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.petdetail.PetPhotoCarouselScreen.State
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.test.TestEventSink
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PetDetailUiTest {
  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val coilRule = CoilRule(FakeImage(100, 100, color = Color.Blue.toArgb()))

  // TODO this seems like not the greatest test pattern, maybe something we can offer better
  //  solutions for via semantics.
  private var carouselScreen: PetPhotoCarouselScreen? = null
  private val circuit =
    Circuit.Builder()
      .setOnUnavailableContent { screen, modifier ->
        when (screen) {
          is PetPhotoCarouselScreen -> {
            PetPhotoCarousel(State(screen), modifier)
            carouselScreen = screen
          }
        }
      }
      .build()

  @Test
  fun petDetail_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { CircuitCompositionLocals(circuit) { PetDetail(PetDetailScreen.State.Loading) } }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petDetail_show_message_for_unknown_animal_state() {
    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuit) { PetDetail(PetDetailScreen.State.UnknownAnimal) }
      }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()

      onNodeWithTag(UNKNOWN_ANIMAL_TAG)
        .assertIsDisplayed()
        .assertTextEquals(Strings.UNKNOWN_ANIMALS)
    }
  }

  @Test
  fun petDetail_show_animal_for_success_state() {
    val success =
      Success(
        id = 1,
        url = "url",
        photoUrls = persistentListOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier",
        tags = persistentListOf("dog", "terrier", "male"),
        eventSink = {},
      )

    val expectedScreen =
      PetPhotoCarouselScreen(
        id = 1,
        name = success.name,
        photoUrls = success.photoUrls,
        photoUrlMemoryCacheKey = null,
      )

    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuit) { ContentWithOverlays { PetDetail(success) } }
      }

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
    val testSink = TestEventSink<Event>()

    val success =
      Success(
        id = 1,
        url = "url",
        photoUrls = persistentListOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier",
        tags = persistentListOf("dog", "terrier", "male"),
        eventSink = testSink,
      )

    val circuit =
      Circuit.Builder()
        .setOnUnavailableContent { screen, modifier ->
          PetPhotoCarousel(State(screen as PetPhotoCarouselScreen), modifier)
        }
        .build()

    composeTestRule.run {
      setContent {
        CircuitCompositionLocals(circuit) { ContentWithOverlays { PetDetail(success) } }
      }

      onNodeWithTag(CAROUSEL_TAG).assertIsDisplayed().performTouchInput { swipeUp() }
      onNodeWithTag(FULL_BIO_TAG, true).assertIsDisplayed().performClick()

      testSink.assertEvent(ViewFullBio(success.url))
    }
  }
}

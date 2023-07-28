// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import coil.annotation.ExperimentalCoilApi
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.star.R
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.petdetail.PetPhotoCarouselTestConstants.CAROUSEL_TAG
import com.slack.circuit.test.TestEventSink
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoilApi::class)
class PetDetailTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  @get:Rule val coilRule = CoilRule(R.drawable.dog2)
  // Not using detectLeaksAfterTestSuccessWrapping() because it causes an NPE with composeTestRule
  @get:Rule val leakDetectionRule = DetectLeaksAfterTestSuccess()

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
        photoUrls = persistentListOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier",
        tags = persistentListOf("dog", "terrier", "male"),
        eventSink = {}
      )

    var carouselScreen: PetPhotoCarouselScreen? = null
    val circuit =
      Circuit.Builder()
        .setOnUnavailableContent { screen, modifier ->
          carouselScreen = screen as PetPhotoCarouselScreen
          PetPhotoCarousel(PetPhotoCarouselScreen.State(screen), modifier)
        }
        .build()

    val expectedScreen =
      PetPhotoCarouselScreen(
        name = success.name,
        photoUrls = success.photoUrls,
        photoUrlMemoryCacheKey = null
      )

    composeTestRule.run {
      setContent { CircuitCompositionLocals(circuit) { PetDetail(success) } }

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
    val testSink = TestEventSink<PetDetailScreen.Event>()

    val success =
      PetDetailScreen.State.Success(
        url = "url",
        photoUrls = persistentListOf("http://some.url"),
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier",
        tags = persistentListOf("dog", "terrier", "male"),
        eventSink = testSink
      )

    val circuit =
      Circuit.Builder()
        .setOnUnavailableContent { screen, modifier ->
          PetPhotoCarousel(PetPhotoCarouselScreen.State(screen as PetPhotoCarouselScreen), modifier)
        }
        .build()

    composeTestRule.run {
      setContent { CircuitCompositionLocals(circuit) { PetDetail(success) } }

      onNodeWithTag(CAROUSEL_TAG).assertIsDisplayed().performTouchInput { swipeUp() }
      onNodeWithTag(FULL_BIO_TAG, true).assertIsDisplayed().performClick()

      testSink.assertEvent(PetDetailScreen.Event.ViewFullBio(success.url))
    }
  }
}

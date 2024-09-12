// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import coil.annotation.ExperimentalCoilApi
import coil3.test.FakeImage
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.db.Gender.MALE
import com.slack.circuit.star.db.Size.SMALL
import com.slack.circuit.star.petlist.PetListScreen.Event
import com.slack.circuit.star.petlist.PetListScreen.Event.ClickAnimal
import com.slack.circuit.star.petlist.PetListScreen.State.Loading
import com.slack.circuit.star.petlist.PetListScreen.State.NoAnimals
import com.slack.circuit.star.petlist.PetListScreen.State.Success
import com.slack.circuit.star.petlist.PetListTestConstants.AGE_AND_BREED_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import com.slack.circuit.test.TestEventSink
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoilApi
@RunWith(RobolectricTestRunner::class)
class PetListUiTest {
  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val coilRule = CoilRule(FakeImage(100, 100, color = Color.Blue.toArgb()))

  @Test
  fun petList_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { PetList(Loading) }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    composeTestRule.run {
      setContent { PetList(NoAnimals(isRefreshing = false)) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()

      onNodeWithTag(NO_ANIMALS_TAG).assertIsDisplayed().assertTextEquals(Strings.NO_ANIMALS)
    }
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = persistentListOf(ANIMAL)

    composeTestRule.run {
      setContent { PetList(Success(animals, isRefreshing = false) {}) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)
      onNodeWithTag(IMAGE_TAG, useUnmergedTree = true).assertIsDisplayed()
      onNodeWithTag(AGE_AND_BREED_TAG, useUnmergedTree = true)
        .assertTextEquals("${ANIMAL.gender.displayName} – ${ANIMAL.age}")
    }
  }

  @Test
  fun petList_emits_event_when_tapping_on_animal() = runTest {
    val testSink = TestEventSink<Event>()
    val animals = persistentListOf(ANIMAL)

    composeTestRule.run {
      setContent { PetList(Success(animals, isRefreshing = false, eventSink = testSink)) }

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)[0].performClick()

      testSink.assertEvent(ClickAnimal(ANIMAL.id, ANIMAL.imageUrl))
    }
  }

  private companion object {
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
  }
}

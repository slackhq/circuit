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
package com.slack.circuit.sample.petlist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.sample.R
import com.slack.circuit.sample.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.sample.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.sample.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.sample.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.sample.petlist.PetListTestConstants.PROGRESS_TAG
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PetListTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun petList_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Loading) {} }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    composeTestRule.run {
      setContent { PetList(PetListScreen.State.NoAnimals) {} }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()

      onNodeWithTag(NO_ANIMALS_TAG)
        .assertIsDisplayed()
        .assertTextEquals(activity.getString(R.string.no_animals))
    }
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = listOf(ANIMAL)

    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Success(animals)) {} }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)
      onNodeWithTag(IMAGE_TAG, true).assertIsDisplayed()
      onNodeWithText(ANIMAL.name).assertIsDisplayed()
      onNodeWithText(ANIMAL.breed ?: "").assertIsDisplayed()
      onNodeWithText("${ANIMAL.gender} – ${ANIMAL.age}").assertIsDisplayed()
    }
  }

  @Test
  fun petList_emits_event_when_tapping_on_animal() = runTest {
    val channel = Channel<Any>(1)
    val animals = listOf(ANIMAL)

    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Success(animals), channel::trySend) }

      onAllNodesWithTag(CARD_TAG)
        .assertCountEquals(1)[0]
        .performClick()

      val event = channel.receive()
      assertThat(event)
        .isEqualTo(PetListScreen.Event.ClickAnimal(ANIMAL.id, ANIMAL.imageUrl))
    }
  }

  private companion object {
    val ANIMAL = PetListAnimal(
      id = 1L,
      name = "Baxter",
      imageUrl = null,
      breed = "Australian Terrier",
      gender = "male",
      age = "12"
    )
  }
}

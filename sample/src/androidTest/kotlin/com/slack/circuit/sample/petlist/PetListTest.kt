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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.data.Breeds
import com.slack.circuit.sample.data.Colors
import com.slack.circuit.sample.data.Link
import com.slack.circuit.sample.data.Links
import com.slack.circuit.sample.data.Photo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PetListTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun petList_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Loading) {} }

      onNodeWithTag("progress").assertIsDisplayed()
      onNodeWithTag("no animals").assertDoesNotExist()
      onNodeWithTag("grid").assertDoesNotExist()
    }
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    composeTestRule.run {
      setContent { PetList(PetListScreen.State.NoAnimals) {} }

      onNodeWithTag("progress").assertDoesNotExist()
      onNodeWithTag("no animals").assertIsDisplayed()
      onNodeWithTag("grid").assertDoesNotExist()
    }
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = listOf(animal.toPetListAnimal())

    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Success(animals)) {} }

      onNodeWithTag("progress").assertDoesNotExist()
      onNodeWithTag("no animals").assertDoesNotExist()

      onNodeWithTag("image", true).assertIsDisplayed()
      onNodeWithTag("name", true).assertIsDisplayed()
      onNodeWithTag("breed", true).assertIsDisplayed()
      onNodeWithTag("gender & age", true).assertIsDisplayed()
    }
  }

  @Test
  fun petList_emits_event_when_tapping_on_animal() = runTest {
    val channel = Channel<Any>(1)
    val petListAnimal = animal.toPetListAnimal()
    val animals = listOf(petListAnimal)

    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Success(animals), channel::trySend) }

      onNode(hasParent(hasTestTag("grid")), true).assertIsDisplayed().performClick()

      val event = channel.receive()
      assertThat(event)
        .isEqualTo(PetListScreen.Event.ClickAnimal(petListAnimal.id, petListAnimal.imageUrl))
    }
  }

  private companion object {
    val animal =
      Animal(
        id = 1L,
        organizationId = "organizationId",
        url = "url",
        type = "type",
        species = "species",
        breeds = Breeds("Australian Terrier"),
        colors = Colors(),
        age = "12",
        size = "size",
        coat = "coat",
        name = "Baxter",
        description = "description",
        gender = "male",
        photos = emptyList(),
        videos = emptyList(),
        status = "status",
        attributes = emptyMap(),
        environment = emptyMap(),
        tags = emptyList(),
        publishedAt = "publishedAt",
        links = Links(self = Link("self"), type = Link("type"), organization = Link("organization"))
      )
  }
}

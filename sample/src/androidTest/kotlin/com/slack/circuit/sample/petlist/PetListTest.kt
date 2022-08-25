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
    composeTestRule.setContent { PetList (PetListScreen.State.Loading) {} }

    composeTestRule.onNodeWithTag("progress").assertIsDisplayed()
    composeTestRule.onNodeWithTag("no animals").assertDoesNotExist()
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    composeTestRule.setContent { PetList (PetListScreen.State.NoAnimals) {} }

    composeTestRule.onNodeWithTag("progress").assertDoesNotExist()
    composeTestRule.onNodeWithTag("no animals").assertIsDisplayed()
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = listOf(animal.toPetListAnimal())

    composeTestRule.setContent { PetList (PetListScreen.State.Success(animals)) {} }

    composeTestRule.onNodeWithTag("progress").assertDoesNotExist()
    composeTestRule.onNodeWithTag("no animals").assertDoesNotExist()

    composeTestRule.onNodeWithTag("image", true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("name", true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("breed", true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("gender & age", true).assertIsDisplayed()
  }

  @Test
  fun petList_emits_event_when_tapping_on_animal() = runTest {
    val channel = Channel<Any>(1)
    val petListAnimal = animal.toPetListAnimal()
    val animals = listOf(petListAnimal)

    composeTestRule.setContent { PetList(PetListScreen.State.Success(animals), channel::trySend) }

    composeTestRule
      .onNode(hasParent(hasTestTag("grid")), true)
      .assertIsDisplayed()
      .performClick()

    val event = channel.receive()
    assertThat(event).isEqualTo(PetListScreen.Event.ClickAnimal(petListAnimal.id, petListAnimal.imageUrl))
  }

  private companion object {
    val photo = Photo(small = "small", medium = "medium", large = "large", full = "full")
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
        photos = listOf(photo),
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

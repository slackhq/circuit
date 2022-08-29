package com.slack.circuit.sample.petdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class PetDetailTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun petDetail_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { PetDetail(PetDetailScreen.State.Loading) }

      onNodeWithTag("progress").assertIsDisplayed()
      onNodeWithTag("unknown animal").assertDoesNotExist()
      onNodeWithTag("animal container").assertDoesNotExist()
    }
  }

  @Test
  fun petDetail_show_message_for_unknown_animal_state() {
    composeTestRule.run {
      setContent { PetDetail(PetDetailScreen.State.UnknownAnimal) }

      onNodeWithTag("progress").assertDoesNotExist()
      onNodeWithTag("unknown animal").assertIsDisplayed()
      onNodeWithTag("animal container").assertDoesNotExist()
    }
  }

  @Test
  fun petDetail_show_animal_for_success_state() {
    val success = PetDetailScreen.State.Success(
      url = "url",
      photoUrl = null,
      photoUrlMemoryCacheKey = null,
      name = "Baxter",
      description = "Grumpy looking Australian Terrier"
    )

    composeTestRule.setContent { PetDetail(success) }

    composeTestRule.onNodeWithTag("progress").assertDoesNotExist()
    composeTestRule.onNodeWithTag("unknown animal").assertDoesNotExist()

    composeTestRule.onNodeWithTag("image").assertIsDisplayed()
    composeTestRule.onNodeWithTag("name").assertIsDisplayed()
    composeTestRule.onNodeWithTag("description").assertIsDisplayed()
  }
}

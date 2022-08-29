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
package com.slack.circuit.sample.petdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class PetDetailTest {
  @get:Rule val composeTestRule = createComposeRule()

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
    val success =
      PetDetailScreen.State.Success(
        url = "url",
        photoUrl = null,
        photoUrlMemoryCacheKey = null,
        name = "Baxter",
        description = "Grumpy looking Australian Terrier"
      )

    composeTestRule.run {
      setContent { PetDetail(success) }

      onNodeWithTag("progress").assertDoesNotExist()
      onNodeWithTag("unknown animal").assertDoesNotExist()

      onNodeWithTag("image").assertIsDisplayed()
      onNodeWithTag("name").assertIsDisplayed()
      onNodeWithTag("description").assertIsDisplayed()
    }
  }
}

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
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.DESCRIPTION_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.IMAGE_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.NAME_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import org.junit.Rule
import org.junit.Test

class PetDetailTest {
  @get:Rule val composeTestRule = createComposeRule()

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
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertIsDisplayed()
      onNodeWithTag(ANIMAL_CONTAINER_TAG).assertDoesNotExist()
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

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(UNKNOWN_ANIMAL_TAG).assertDoesNotExist()

      onNodeWithTag(IMAGE_TAG).assertIsDisplayed()
      onNodeWithTag(NAME_TAG).assertIsDisplayed()
      onNodeWithTag(DESCRIPTION_TAG).assertIsDisplayed()
    }
  }
}

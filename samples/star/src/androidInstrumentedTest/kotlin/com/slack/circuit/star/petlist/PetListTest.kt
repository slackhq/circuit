// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.db.Gender.MALE
import com.slack.circuit.star.db.Size.SMALL
import com.slack.circuit.star.petlist.PetListScreen.State.Loading
import com.slack.circuit.star.petlist.PetListScreen.State.NoAnimals
import com.slack.circuit.star.petlist.PetListScreen.State.Success
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import com.slack.circuit.star.resources.Res
import com.slack.circuit.star.resources.dog
import com.slack.circuit.test.TestEventSink
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PetListTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule val coilRule = CoilRule(Res.drawable.dog)

  @Test
  fun petList_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setTestContent { PetList(Loading) }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    composeTestRule.run {
      setTestContent { PetList(NoAnimals(isRefreshing = false)) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()

      onNodeWithTag(NO_ANIMALS_TAG).assertIsDisplayed().assertTextEquals(Strings.NO_ANIMALS)
    }
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = persistentListOf(ANIMAL)

    composeTestRule.run {
      setTestContent { PetList(Success(animals, isRefreshing = false) {}) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)
      onNodeWithTag(IMAGE_TAG, true).assertIsDisplayed()
      onNodeWithText(ANIMAL.name).assertIsDisplayed()
      onNodeWithText(ANIMAL.breed.orEmpty()).assertIsDisplayed()
      onNodeWithText("${ANIMAL.gender.displayName} – ${ANIMAL.age}").assertIsDisplayed()
    }
  }

  @Test
  fun petList_emits_event_when_tapping_on_animal() = runTest {
    val testSink = TestEventSink<PetListScreen.Event>()
    val animals = persistentListOf(ANIMAL)

    composeTestRule.run {
      setTestContent { PetList(Success(animals, isRefreshing = false, eventSink = testSink)) }

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)[0].performClick()

      testSink.assertEvent(PetListScreen.Event.ClickAnimal(ANIMAL.id, ANIMAL.imageUrl))
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

@OptIn(ExperimentalSharedTransitionApi::class)
private fun ComposeContentTestRule.setTestContent(content: @Composable () -> Unit) {
  setContent { PreviewSharedElementTransitionLayout { content() } }
}

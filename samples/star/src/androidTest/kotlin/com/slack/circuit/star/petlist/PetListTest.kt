// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil.annotation.ExperimentalCoilApi
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.sample.coil.test.CoilRule
import com.slack.circuit.star.R
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@OptIn(ExperimentalCoilApi::class)
class PetListTest {
  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private val coilRule = CoilRule(R.drawable.dog)

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
      around(coilRule)
    }

  @Test
  fun petList_show_progress_indicator_for_loading_state() {
    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Loading) }

      onNodeWithTag(PROGRESS_TAG).assertIsDisplayed()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()
    }
  }

  @Test
  fun petList_show_message_for_no_animals_state() {
    composeTestRule.run {
      setContent { PetList(PetListScreen.State.NoAnimals(isRefreshing = false)) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(GRID_TAG).assertDoesNotExist()

      onNodeWithTag(NO_ANIMALS_TAG)
        .assertIsDisplayed()
        .assertTextEquals(activity.getString(R.string.no_animals))
    }
  }

  @Test
  fun petList_show_list_for_success_state() {
    val animals = persistentListOf(ANIMAL)

    composeTestRule.run {
      setContent { PetList(PetListScreen.State.Success(animals, isRefreshing = false) {}) }

      onNodeWithTag(PROGRESS_TAG).assertDoesNotExist()
      onNodeWithTag(NO_ANIMALS_TAG).assertDoesNotExist()

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)
      onNodeWithTag(IMAGE_TAG, true).assertIsDisplayed()
      onNodeWithText(ANIMAL.name).assertIsDisplayed()
      onNodeWithText(ANIMAL.breed.orEmpty()).assertIsDisplayed()
      onNodeWithText("${ANIMAL.gender} – ${ANIMAL.age}").assertIsDisplayed()
    }
  }

  @Test
  fun petList_emits_event_when_tapping_on_animal() = runTest {
    val channel = Channel<Any>(1)
    val animals = persistentListOf(ANIMAL)

    composeTestRule.run {
      setContent {
        PetList(
          PetListScreen.State.Success(animals, isRefreshing = false, eventSink = channel::trySend)
        )
      }

      onAllNodesWithTag(CARD_TAG).assertCountEquals(1)[0].performClick()

      val event = channel.receive()
      assertThat(event).isEqualTo(PetListScreen.Event.ClickAnimal(ANIMAL.id, ANIMAL.imageUrl))
    }
  }

  private companion object {
    val ANIMAL =
      PetListAnimal(
        id = 1L,
        name = "Baxter",
        imageUrl = "http://some.url",
        breed = "Australian Terrier",
        gender = Gender.MALE,
        size = Size.SMALL,
        age = "12"
      )
  }
}

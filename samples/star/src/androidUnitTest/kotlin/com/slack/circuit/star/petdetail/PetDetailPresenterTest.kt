// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.petdetail.PetDetailScreen.Event.ViewFullBio
import com.slack.circuit.star.petdetail.PetDetailScreen.State
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Full
import com.slack.circuit.star.petlist.PetListPresenterTest
import com.slack.circuit.star.petlist.TestRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PetDetailPresenterTest {
  @Test
  fun `present - emit loading state then no animal state`() = runTest {
    val repository = TestRepository(emptyList())
    val screen = PetDetailScreen(123L, "key")
    val navigator = FakeNavigator(screen)
    val presenter = PetDetailPresenter(screen, navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(State.Loading)
      assertThat(awaitItem()).isEqualTo(State.UnknownAnimal)
    }
  }

  @Test
  fun `present - emit loading state then success state`() = runTest {
    val animal = PetListPresenterTest.animal
    val repository = TestRepository(listOf(animal))
    val screen = PetDetailScreen(animal.id, animal.primaryPhotoUrl)
    val navigator = FakeNavigator(screen)
    val presenter = PetDetailPresenter(screen, navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(State.Loading)

      val success = awaitItem()
      check(success is Full)

      val expected = animal.toPetDetailState(animal.primaryPhotoUrl, eventSink = success.eventSink)
      assertThat(success).isEqualTo(expected)
    }
  }

  @Test
  fun `present - navigate using custom tab intent screen`() = runTest {
    val animal = PetListPresenterTest.animal
    val repository = TestRepository(listOf(animal))
    val screen = PetDetailScreen(animal.id, animal.primaryPhotoUrl)
    val navigator = FakeNavigator(screen)
    val presenter = PetDetailPresenter(screen, navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(State.Loading)

      val successState = awaitItem()
      check(successState is Full)

      successState.eventSink(ViewFullBio(animal.url))
      assertThat(navigator.awaitNextScreen()).isEqualTo(OpenUrlScreen(animal.url))
    }
  }
}

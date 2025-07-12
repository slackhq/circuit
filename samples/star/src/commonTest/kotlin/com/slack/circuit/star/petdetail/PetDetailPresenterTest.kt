// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.star.BasePresenterTest
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.petlist.PetListPresenterTest
import com.slack.circuit.star.petlist.TestRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class PetDetailPresenterTest : BasePresenterTest() {
  @Test
  fun `present - emit loading state then no animal state`() = runTest {
    val repository = TestRepository(emptyList())
    val screen = PetDetailScreen(123L, "key")
    val navigator = FakeNavigator(screen)
    val presenter = PetDetailPresenter(screen, navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(PetDetailScreen.State.Loading)
      assertThat(awaitItem()).isEqualTo(PetDetailScreen.State.UnknownAnimal)
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
      assertThat(awaitItem()).isEqualTo(PetDetailScreen.State.Loading)

      val success = awaitItem()
      check(success is PetDetailScreen.State.Full)

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
      assertThat(awaitItem()).isEqualTo(PetDetailScreen.State.Loading)

      val successState = awaitItem()
      check(successState is PetDetailScreen.State.Full)

      successState.eventSink(PetDetailScreen.Event.ViewFullBio(animal.url))
      assertThat(navigator.awaitNextScreen()).isEqualTo(OpenUrlScreen(animal.url))
    }
  }
}

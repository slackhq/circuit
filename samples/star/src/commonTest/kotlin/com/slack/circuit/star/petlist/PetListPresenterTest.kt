// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.star.BasePresenterTest
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petlist.PetListScreen.Event.ClickAnimal
import com.slack.circuit.star.petlist.PetListScreen.State.Loading
import com.slack.circuit.star.petlist.PetListScreen.State.NoAnimals
import com.slack.circuit.star.petlist.PetListScreen.State.Success
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest

class PetListPresenterTest : BasePresenterTest() {
  private val navigator = FakeNavigator(PetListScreen)

  @Test
  fun `present - emit loading state then no animals state`() = runTest {
    val repository = TestRepository(emptyList())
    val presenter = PetListPresenter(navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(Loading)
      assertThat(awaitItem()).isEqualTo(NoAnimals(isRefreshing = false))
    }
  }

  @Test
  fun `present - emit loading state then list of animals`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(Loading)

      val animals = listOf(animal.toPetListAnimal())
      val state = awaitItem()
      check(state is Success)
      assertThat(state.animals).isEqualTo(animals)
    }
  }

  @Test
  fun `present - navigate to pet details screen`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, repository)

    val petListAnimal = animal.toPetListAnimal()
    presenter.test {
      assertThat(Loading).isEqualTo(awaitItem())
      val successState = awaitItem()
      check(successState is Success)
      assertThat(successState.animals).isEqualTo(listOf(petListAnimal))

      val clickAnimal = ClickAnimal(123L, "key", petListAnimal)
      successState.eventSink(clickAnimal)
      assertThat(navigator.awaitNextScreen())
        .isEqualTo(
          PetDetailScreen(
            petId = clickAnimal.petId,
            photoUrlMemoryCacheKey = clickAnimal.photoUrlMemoryCacheKey,
            animal = clickAnimal.animal.toPartialAnimal(),
          )
        )
    }
  }

  companion object {
    val animal =
      Animal(
        id = 1L,
        name = "name",
        primaryPhotoUrl = "https://example.com/photo.png",
        primaryPhotoAspectRatio = 1.33,
        primaryBreed = "Shepherd",
        gender = Gender.MALE,
        size = Size.SMALL,
        description = "description",
        photoUrls = listOf("https://example.com/photo.png"),
        sort = 0,
        tags = listOf("tag"),
        url = "https://example.com",
        age = "Adult",
      )
  }
}

class TestRepository(private val animals: List<Animal>) : PetRepository {
  override suspend fun refreshData() {}

  override fun animalsFlow(): Flow<List<Animal>> = flow { emit(animals) }

  override suspend fun getAnimal(id: Long): Animal? = animals.firstOrNull { it.id == id }
}

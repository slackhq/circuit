// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PetListPresenterTest {
  private val navigator = FakeNavigator()

  @Test
  fun `present - emit loading state then no animals state`() = runTest {
    val repository = TestRepository(emptyList())
    val presenter = PetListPresenter(navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(PetListScreen.State.Loading)
      assertThat(awaitItem()).isEqualTo(PetListScreen.State.NoAnimals(isRefreshing = false))
    }
  }

  @Test
  fun `present - emit loading state then list of animals`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(PetListScreen.State.Loading)

      val animals = listOf(animal.toPetListAnimal())
      val state = awaitItem()
      check(state is PetListScreen.State.Success)
      assertThat(state.animals).isEqualTo(animals)
    }
  }

  @Test
  fun `present - navigate to pet details screen`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, repository)

    presenter.test {
      assertThat(PetListScreen.State.Loading).isEqualTo(awaitItem())
      val successState = awaitItem()
      check(successState is PetListScreen.State.Success)
      assertThat(successState.animals).isEqualTo(listOf(animal).map { it.toPetListAnimal() })

      val clickAnimal = PetListScreen.Event.ClickAnimal(123L, "key")
      successState.eventSink(clickAnimal)
      assertThat(navigator.awaitNextScreen())
        .isEqualTo(PetDetailScreen(clickAnimal.petId, clickAnimal.photoUrlMemoryCacheKey))
    }
  }

  companion object {
    val animal =
      Animal(
        id = 1L,
        name = "name",
        primaryPhotoUrl = "https://example.com/photo.png",
        primaryBreed = "Shepherd",
        gender = Gender.MALE,
        size = Size.SMALL,
        description = "description",
        photoUrls = persistentListOf("https://example.com/photo.png"),
        sort = 0,
        tags = persistentListOf("tag"),
        url = "https://example.com",
        age = "Adult",
      )
  }
}

class TestRepository(private val animals: List<Animal>) : PetRepository {
  override suspend fun refreshData() {
    // Do nothing
  }

  override fun animalsFlow(): Flow<List<Animal>> = flow { emit(animals) }

  override suspend fun getAnimal(id: Long): Animal? = animals.firstOrNull { it.id == id }

  override suspend fun getAnimalBio(id: Long): String? = getAnimal(id)?.description
}

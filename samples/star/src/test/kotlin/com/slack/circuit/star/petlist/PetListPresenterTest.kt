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
package com.slack.circuit.star.petlist

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.star.data.Animal
import com.slack.circuit.star.data.Breeds
import com.slack.circuit.star.data.Colors
import com.slack.circuit.star.data.Link
import com.slack.circuit.star.data.Links
import com.slack.circuit.star.data.Photo
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.test
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
    val presenter = PetListPresenter(navigator, PetListScreen(), repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(PetListScreen.State.Loading)
      assertThat(awaitItem()).isEqualTo(PetListScreen.State.NoAnimals(isRefreshing = false))
    }
  }

  @Test
  fun `present - emit loading state then list of animals`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, PetListScreen(), repository)

    presenter.test {
      assertThat(awaitItem()).isEqualTo(PetListScreen.State.Loading)

      val animals = listOf(animal).map { it.toPetListAnimal() }
      val state = awaitItem()
      check(state is PetListScreen.State.Success)
      assertThat(state.animals).isEqualTo(animals)
    }
  }

  @Test
  fun `present - navigate to pet details screen`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, PetListScreen(), repository)

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
    val photo = Photo(small = "small", medium = "medium", large = "large", full = "full")
    val animal =
      Animal(
        id = 1L,
        organizationId = "organizationId",
        url = "url",
        type = "type",
        species = "species",
        breeds = Breeds(),
        colors = Colors(),
        age = "age",
        size = "size",
        coat = "coat",
        name = "name",
        description = "description",
        gender = "gender",
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

class TestRepository(private val animals: List<Animal>) : PetRepository {
  override suspend fun getAnimals(forceRefresh: Boolean): List<Animal> = animals
  override suspend fun getAnimal(id: Long): Animal? = animals.firstOrNull { it.id == id }
}

// TODO move this to test artifact
class FakeNavigator : Navigator {
  private val navigatedScreens = Turbine<Screen>()
  private val pops = Turbine<Unit>()

  override fun goTo(screen: Screen) {
    navigatedScreens.add(screen)
  }

  override fun pop(): Screen? {
    pops.add(Unit)
    return null
  }

  // For non-coroutines users only
  fun takeNextScreen() = navigatedScreens.takeItem()

  suspend fun awaitNextScreen() = navigatedScreens.awaitItem()

  suspend fun awaitPop(): Unit = pops.awaitItem()

  fun assertIsEmpty() {
    navigatedScreens.ensureAllEventsConsumed()
  }

  fun expectNoEvents() {
    navigatedScreens.expectNoEvents()
  }
}

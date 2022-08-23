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
package com.slack.circuit.sample.petlist

import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.data.Breeds
import com.slack.circuit.sample.data.Colors
import com.slack.circuit.sample.data.Link
import com.slack.circuit.sample.data.Links
import com.slack.circuit.sample.repo.PetRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PetListPresenterTest {
  private val navigator = TestNavigator()

  @Test
  fun `present - emit loading state then no animals state`() = runTest {
    val repository = TestRepository(emptyList())
    val presenter = PetListPresenter(navigator, repository)
    val events = MutableSharedFlow<PetListScreen.Event>()

    moleculeFlow(Immediate) { presenter.present(events) }
      .test {
        assertThat(PetListScreen.State.Loading).isEqualTo(awaitItem())
        assertThat(PetListScreen.State.NoAnimals).isEqualTo(awaitItem())
      }
  }

  @Test
  fun `present - emit loading state then list of animals`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, repository)
    val events = MutableSharedFlow<PetListScreen.Event>()

    moleculeFlow(Immediate) { presenter.present(events) }
      .test {
        assertThat(PetListScreen.State.Loading).isEqualTo(awaitItem())

        val animals = listOf(animal).map { it.toPetListAnimal() }
        assertThat(PetListScreen.State.Success(animals)).isEqualTo(awaitItem())
      }
  }

  @Test
  fun `present - navigate to pet details screen`() = runTest {
    val repository = TestRepository(emptyList())
    val presenter = PetListPresenter(navigator, repository)
    val events = Channel<PetListScreen.Event>(Channel.BUFFERED)

    moleculeFlow(Immediate) { presenter.present(events.receiveAsFlow()) }
      .test {
        assertThat(PetListScreen.State.Loading).isEqualTo(awaitItem())
        assertThat(PetListScreen.State.NoAnimals).isEqualTo(awaitItem())

        val clickAnimal = PetListScreen.Event.ClickAnimal(123L)
        events.send(clickAnimal)
      }
    navigator.screens().test {
      // Hangs forever waiting
      //      assertThat(PetDetailScreen(123L)).isEqualTo(awaitItem())
    }
  }

  private companion object {
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
        photos = emptyList(),
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

private class TestRepository(animals: List<Animal>) : PetRepository {
  private val _animalStateFlow = MutableStateFlow(emptyList<Animal>()).apply { tryEmit(animals) }
  override val animalsStateFlow: StateFlow<List<Animal>> = _animalStateFlow

  override fun getAnimal(id: Long): Animal = TODO("Not yet implemented")
}

// TODO expose the backstack directly here somehow and assert the stack?
private class TestNavigator : Navigator {
  private val screens = MutableSharedFlow<Screen>()
  private val pops = MutableSharedFlow<Unit>()

  override fun goTo(screen: Screen) {
    screens.tryEmit(screen)
  }

  override fun pop() {
    pops.tryEmit(Unit)
  }

  fun screens(): Flow<Screen> = screens

  fun takePop(): Flow<Unit> = pops
}

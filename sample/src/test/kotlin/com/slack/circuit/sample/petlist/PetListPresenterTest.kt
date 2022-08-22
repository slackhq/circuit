package com.slack.circuit.sample.petlist

import android.os.Parcelable
import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock.Immediate
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.StateRenderer
import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.data.Breeds
import com.slack.circuit.sample.data.Colors
import com.slack.circuit.sample.data.Link
import com.slack.circuit.sample.data.Links
import com.slack.circuit.sample.petdetail.PetDetailScreen
import com.slack.circuit.sample.repo.PetRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class PetListPresenterTest {
  private lateinit var navigator: TestNavigator

  @Before
  fun setup() {
    navigator = TestNavigator()
  }

  @Test
  fun `present - emit loading state then no animals state`() = runTest {
    val repository = TestRepository(emptyList())
    val presenter = PetListPresenter(navigator, repository)
    val renderer = TestRenderer<PetListScreen.State, PetListScreen.Event>()

    moleculeFlow(Immediate) {
      presenter.present(renderer.events)
    }.test {
      assertEquals(PetListScreen.State.Loading, awaitItem())
      assertEquals(PetListScreen.State.NoAnimals, awaitItem())
    }
  }

  @Test
  fun `present - emit loading state then list of animals`() = runTest {
    val repository = TestRepository(listOf(animal))
    val presenter = PetListPresenter(navigator, repository)
    val renderer = TestRenderer<PetListScreen.State, PetListScreen.Event>()

    moleculeFlow(Immediate) {
      presenter.present(renderer.events)
    }.test {
      assertEquals(PetListScreen.State.Loading, awaitItem())

      val animals = listOf(animal).map { it.toPetListAnimal() }
      assertEquals(PetListScreen.State.Success(animals), awaitItem())
    }
  }

  @Test
  fun `present - navigate to pet details screen`() = runTest {
    val repository = TestRepository(emptyList())
    val presenter = PetListPresenter(navigator, repository)
    val renderer = TestRenderer<PetListScreen.State, PetListScreen.Event>()

    moleculeFlow(Immediate) {
      presenter.present(renderer.events)
    }.test {
      val clickAnimal = PetListScreen.Event.ClickAnimal(123L)
      renderer.emitEvent(clickAnimal)

      // TODO how do we watch for event triggered behaviour that does NOT result in the emission
      // TODO of new state??
      navigator.assertGoTo(PetDetailScreen(123L))
    }
  }

  private companion object {
    val animal = Animal(
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
      links = Links(
        self = Link("self"),
        type = Link("type"),
        organization = Link("organization")
      )
    )
  }
}

private class TestRenderer<UiState, UiEvent> : StateRenderer<UiState, UiEvent>
        where UiState : Any, UiState : Parcelable, UiEvent : Any
{
  private val _events = MutableSharedFlow<UiEvent>()
  override val events = _events.asSharedFlow()

  fun emitEvent(event: UiEvent): Boolean = _events.tryEmit(event)

  @Composable
  override fun render(state: UiState) = TODO("Not yet implemented")
}

private class TestRepository(animals: List<Animal>) : PetRepository {
  private val _animalStateFlow = MutableStateFlow(emptyList<Animal>()).apply { tryEmit(animals) }
  override val animalsStateFlow: StateFlow<List<Animal>> = _animalStateFlow

  override fun getAnimal(id: Long): Animal = TODO("Not yet implemented")
}

private class TestNavigator : Navigator {
  private var receivedGoToScreen: Screen? = null

  override fun goTo(screen: Screen) {
    receivedGoToScreen = screen
  }

  override fun pop() = TODO("Not yet implemented")

  fun assertGoTo(screen: Screen, lazyMessage: (() -> String)? = null) {
    if (receivedGoToScreen != screen) fail(lazyMessage?.invoke())
  }
}

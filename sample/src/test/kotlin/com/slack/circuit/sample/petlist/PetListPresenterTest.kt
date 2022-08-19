package com.slack.circuit.sample.petlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import app.cash.molecule.RecompositionClock.*
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
import com.slack.circuit.sample.repo.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
//@RunWith(RobolectricTestRunner::class)
class PetListPresenterTest {
  private val animal = Animal(
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

  lateinit var navigator: Navigator

  @Before
  fun setup() {
    navigator = TestNavigator()
  }

  @Test
  fun `present - emit loading state then list of animals`() = runTest {
    val repository = TestRepository(listOf(animal))
    val renderer = TestRenderer()
    val presenter = PetListPresenter(navigator, repository)

    moleculeFlow(Immediate) {
      presenter.present(renderer)
      renderer.state
    }.test {
      assertEquals(PetListScreen.State.Loading, awaitItem())

      val petListAnimals = listOf(animal).map { it.toPetListAnimal() }
      assertEquals(PetListScreen.State.Success(petListAnimals), awaitItem())
    }
  }

}

private class TestRenderer : StateRenderer<PetListScreen.State, PetListScreen.Event> {
  var state: PetListScreen.State? = null
    private set

  @Composable
  override fun render(state: PetListScreen.State, uiEvents: (PetListScreen.Event) -> Unit) {
    this.state = state
  }
}

private class TestRepository(animals: List<Animal>) : PetRepository {
  private val _animalStateFlow = MutableStateFlow(emptyList<Animal>()).apply { tryEmit(animals) }
  override val animalsStateFlow: StateFlow<List<Animal>> = _animalStateFlow

  override fun getAnimal(id: Long): Animal {
    TODO("Not yet implemented")
  }
}

private class TestNavigator : Navigator {
  private var receivedGoToScreen: Screen? = null

  override fun goTo(screen: Screen) {
    receivedGoToScreen = screen
  }

  override fun pop() {
    TODO("Not yet implemented")
  }

  fun assertGoTo(screen: Screen, lazyMessage: (() -> String)? = null) {
    if (receivedGoToScreen != screen) fail(lazyMessage?.invoke())
  }
}

package com.slack.circuit.sample.petlist

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.slack.circuit.ContentContainer
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.StateRenderer
import com.slack.circuit.ui
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@Parcelize
data class PetListAnimal(
  val id: String,
  val name: String,
) : Parcelable

@Parcelize
object PetListScreen : Screen {
  sealed interface State : Parcelable {
    @Parcelize object Loading : State
    @Parcelize data class Success(val animals: List<PetListAnimal>) : State
  }

  sealed interface Event {
    data class ClickAnimal(val id: String) : Event
  }
}

class PetListScreenPresenterFactory
@Inject
constructor(private val petListPresenterFactory: PetListPresenter.Factory) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is PetListScreen) return petListPresenterFactory.create(navigator)
    return null
  }
}

class PetListPresenter @AssistedInject constructor(@Assisted private val navigator: Navigator) :
  Presenter<PetListScreen.State, PetListScreen.Event> {
  @Composable
  override fun present(render: StateRenderer<PetListScreen.State, PetListScreen.Event>) {
    var state by rememberSaveable {
      mutableStateOf(PetListScreen.State.Success(listOf(PetListAnimal("id", "Moose"))))
      // mutableStateOf(PetListScreen.State.Loading )
    }

    val context = LocalContext.current
    render(state) { event ->
      when (event) {
        is PetListScreen.Event.ClickAnimal -> {
          // TODO!!!!
          // navigator.goTo(PetDetailScreen(event.id))
          Toast.makeText(context, "Going to ${event.id}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): PetListPresenter
  }
}

@Module
interface PetListModule {
  @Binds @IntoSet fun PetListScreenFactory.bindPetListScreenFactory(): ScreenViewFactory
  @Binds @IntoSet fun PetListScreenPresenterFactory.bindPetListScreenPresenterFactory(): PresenterFactory
}

class PetListScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen, container: ContentContainer): ScreenView? {
    if (screen is PetListScreen) {
      return ScreenView(container, petListUi())
    }
    return null
  }
}

private fun petListUi() =
  ui<PetListScreen.State, PetListScreen.Event> { state, events -> renderImpl(state, events) }

@Composable
private fun renderImpl(state: PetListScreen.State, events: (PetListScreen.Event) -> Unit) {
  when (state) {
    PetListScreen.State.Loading -> {
      // TODO loading view
    }
    is PetListScreen.State.Success -> {
      PetList(animals = state.animals, events = events)
    }
  }
}

@Composable
private fun PetList(animals: List<PetListAnimal>, events: (PetListScreen.Event) -> Unit) {
  LazyColumn {
    animals.forEach { animal ->
      item { PetListItem(animal) { events(PetListScreen.Event.ClickAnimal(animal.id)) } }
    }
  }
}

@Composable
private fun PetListItem(animal: PetListAnimal, onClick: () -> Unit) {
  Text(modifier = Modifier.clickable { onClick() }, text = animal.name)
}

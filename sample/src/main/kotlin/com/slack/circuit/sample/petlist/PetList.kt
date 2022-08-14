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

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.slack.circuit.ContentContainer
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.StateRenderer
import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.petdetail.PetDetailScreen
import com.slack.circuit.sample.repo.PetRepository
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
  val id: Long,
  val name: String,
  val description: String,
) : Parcelable

@Parcelize
object PetListScreen : Screen {
  sealed interface State : Parcelable {
    @Parcelize object Loading : State
    @Parcelize data class Success(val animals: List<PetListAnimal>) : State
  }

  sealed interface Event {
    data class ClickAnimal(val petId: Long) : Event
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

class PetListPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val petRepo: PetRepository,
) : Presenter<PetListScreen.State, PetListScreen.Event> {
  @Composable
  override fun present(render: StateRenderer<PetListScreen.State, PetListScreen.Event>) {
    val repoState by petRepo.animalsStateFlow.collectAsState()
    // TODO revisit why we can't use rememberSavable here
    val state by remember {
      derivedStateOf {
        if (repoState.isEmpty()) {
          PetListScreen.State.Loading
        } else {
          repoState.map { it.toPetListAnimal() }.let { PetListScreen.State.Success(it) }
        }
      }
    }

    render(state) { event ->
      when (event) {
        is PetListScreen.Event.ClickAnimal -> {
          navigator.goTo(PetDetailScreen(event.petId))
        }
      }
    }
  }

  private fun Animal.toPetListAnimal(): PetListAnimal {
    return PetListAnimal(id = id, name = name, description = description)
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): PetListPresenter
  }
}

@Module
interface PetListModule {
  @Binds @IntoSet fun PetListScreenFactory.bindPetListScreenFactory(): ScreenViewFactory
  @Binds
  @IntoSet
  fun PetListScreenPresenterFactory.bindPetListScreenPresenterFactory(): PresenterFactory
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
  ui<PetListScreen.State, PetListScreen.Event> { state, events -> RenderImpl(state, events) }

@Composable
private fun RenderImpl(state: PetListScreen.State, events: (PetListScreen.Event) -> Unit) {
  Scaffold(
    modifier = Modifier.systemBarsPadding().systemGesturesPadding().fillMaxWidth(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
      )
    },
  ) { paddingValues ->
    when (state) {
      PetListScreen.State.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      is PetListScreen.State.Success -> {
        PetList(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          animals = state.animals,
          events = events
        )
      }
    }
  }
}

@Composable
private fun PetList(
  modifier: Modifier = Modifier,
  animals: List<PetListAnimal>,
  events: (PetListScreen.Event) -> Unit
) {
  LazyColumn(modifier) {
    animals.forEach { animal ->
      item { PetListItem(animal) { events(PetListScreen.Event.ClickAnimal(animal.id)) } }
    }
  }
}

@Composable
private fun PetListItem(animal: PetListAnimal, onClick: () -> Unit) {
  Text(modifier = Modifier.clickable { onClick() }, text = animal.name)
}

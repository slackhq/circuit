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
package com.slack.circuit.sample.petdetail

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.slack.circuit.ContentContainer
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.StateRenderer
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.repo.PetRepository
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@Parcelize
data class PetDetailScreen(val petId: Long) : Screen {
  @Parcelize
  data class State(
    val url: String,
    val photoUrl: String,
    val name: String,
    val description: String,
  ) : Parcelable
}

@ContributesMultibinding(AppScope::class)
class PetDetailScreenPresenterFactory
@Inject
constructor(private val petDetailPresenterFactory: PetDetailPresenter.Factory) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is PetDetailScreen) return petDetailPresenterFactory.create(screen)
    return null
  }
}

class PetDetailPresenter
@AssistedInject
constructor(
  @Assisted private val screen: PetDetailScreen,
  private val petRepository: PetRepository
) : Presenter<PetDetailScreen.State, Nothing> {
  @Composable
  override fun present(render: StateRenderer<PetDetailScreen.State, Nothing>) {
    val animal = petRepository.getAnimal(screen.petId)
    val state by rememberSaveable {
      mutableStateOf(
        PetDetailScreen.State(
          url = animal.url,
          photoUrl = animal.photos.first().large,
          name = animal.name,
          description = animal.description
        )
      )
    }

    render(state) { /* nothing to do yet! */}
  }

  @AssistedFactory
  interface Factory {
    fun create(screen: PetDetailScreen): PetDetailPresenter
  }
}

@ContributesMultibinding(AppScope::class)
class PetDetailScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen, container: ContentContainer): ScreenView? {
    if (screen is PetDetailScreen) return ScreenView(container, petDetailUi())
    return null
  }
}

private fun petDetailUi() = ui<PetDetailScreen.State, Nothing> { state, _ -> renderImpl(state) }

@Composable
private fun renderImpl(state: PetDetailScreen.State) {
  Scaffold(
    modifier = Modifier.systemBarsPadding(),
  ) { padding ->
    LazyColumn(modifier = Modifier.padding(padding)) {
      item {
        SubcomposeAsyncImage(
          modifier = Modifier.fillMaxWidth(),
          model = state.photoUrl,
          contentDescription = state.name,
          contentScale = ContentScale.FillWidth,
          loading = { CircularProgressIndicator() }
        )
      }
      item { Text(text = state.name, style = MaterialTheme.typography.displayLarge) }
      item { Text(text = state.description) }
    }
  }
}

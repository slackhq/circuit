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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.slack.circuit.ContentContainer
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.repo.PetRepository
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Parcelize
data class PetDetailScreen(val petId: Long, val photoUrlMemoryCacheKey: String) : Screen {
  sealed interface State : Parcelable {
    @Parcelize object Loading : State
    @Parcelize object NoAnimal : State
    @Parcelize
    data class Success(
      val url: String,
      val photoUrl: String,
      val photoUrlMemoryCacheKey: String,
      val name: String,
      val description: String,
    ) : State
  }
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
  override fun present(events: Flow<Nothing>): PetDetailScreen.State {
    val state =
      produceState<PetDetailScreen.State>(PetDetailScreen.State.Loading) {
        val animal = petRepository.getAnimal(screen.petId)
        value =
          when (animal) {
            null -> PetDetailScreen.State.NoAnimal
            else -> {
              PetDetailScreen.State.Success(
                url = animal.url,
                photoUrl = animal.photos.first().large,
                photoUrlMemoryCacheKey = screen.photoUrlMemoryCacheKey,
                name = animal.name,
                description = animal.description
              )
            }
          }
      }

    //    LaunchedEffect(this) { /* nothing to do yet */ }

    return state.value
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
  Scaffold(modifier = Modifier.systemBarsPadding()) { padding ->
    when (state) {
      PetDetailScreen.State.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
      PetDetailScreen.State.NoAnimal -> TODO()
      is PetDetailScreen.State.Success -> {
        LazyColumn(modifier = Modifier.padding(padding)) {
          item {
            AsyncImage(
              modifier = Modifier.fillMaxWidth(),
              model =
                ImageRequest.Builder(LocalContext.current)
                  .data(state.photoUrl)
                  .placeholderMemoryCacheKey(state.photoUrlMemoryCacheKey)
                  .crossfade(true)
                  .build(),
              contentDescription = state.name,
              contentScale = ContentScale.FillWidth,
            )
          }
          item { Text(text = state.name, style = MaterialTheme.typography.displayLarge) }
          item { Text(text = state.description) }
        }
      }
    }
  }
}

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

import android.content.res.Configuration
import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.slack.circuit.CircuitContent
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.sample.R
import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.sample.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
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
data class PetDetailScreen(val petId: Long, val photoUrlMemoryCacheKey: String?) : Screen {
  sealed interface State : Parcelable {
    @Parcelize object Loading : State
    @Parcelize object UnknownAnimal : State
    @Parcelize
    data class Success(
      val url: String,
      val photoUrls: List<String>,
      val photoUrlMemoryCacheKey: String?,
      val name: String,
      val description: String,
    ) : State
  }
}

internal fun Animal.toPetDetailState(photoUrlMemoryCacheKey: String?): PetDetailScreen.State {
  return PetDetailScreen.State.Success(
    url = url,
    photoUrls = photos.map { it.large },
    photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
    name = name,
    description = description
  )
}

@ContributesMultibinding(AppScope::class)
class PetDetailScreenPresenterFactory
@Inject
constructor(
  private val petDetailPresenterFactory: PetDetailPresenter.Factory,
  private val petPhotoCarousel: PetPhotoCarouselPresenter.Factory
) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is PetDetailScreen) return petDetailPresenterFactory.create(screen)
    if (screen is PetPhotoCarouselScreen) return petPhotoCarousel.create(screen)
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
    val state by
      produceState<PetDetailScreen.State>(PetDetailScreen.State.Loading) {
        val animal = petRepository.getAnimal(screen.petId)
        value =
          when (animal) {
            null -> PetDetailScreen.State.UnknownAnimal
            else -> animal.toPetDetailState(screen.photoUrlMemoryCacheKey)
          }
      }

    return state
  }

  @AssistedFactory
  interface Factory {
    fun create(screen: PetDetailScreen): PetDetailPresenter
  }
}

@ContributesMultibinding(AppScope::class)
class PetDetailScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen): ScreenView? {
    if (screen is PetDetailScreen) return ScreenView(petDetailUi())
    return null
  }
}

private fun petDetailUi() = ui<PetDetailScreen.State, Nothing> { state, _ -> PetDetail(state) }

internal object PetDetailTestConstants {
  const val ANIMAL_CONTAINER_TAG = "animal_container"
  const val PROGRESS_TAG = "progress"
  const val UNKNOWN_ANIMAL_TAG = "unknown_animal"
}

@Composable
internal fun PetDetail(state: PetDetailScreen.State) {
  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  Scaffold(modifier = Modifier.systemBarsPadding()) { padding ->
    when (state) {
      PetDetailScreen.State.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.testTag(PROGRESS_TAG))
        }
      }
      PetDetailScreen.State.UnknownAnimal -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            modifier = Modifier.testTag(UNKNOWN_ANIMAL_TAG),
            text = stringResource(id = R.string.unknown_animals)
          )
        }
      }
      is PetDetailScreen.State.Success -> {
        if (isLandscape) {
          Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            CircuitContent(
              PetPhotoCarouselScreen(
                name = state.name,
                photoUrls = state.photoUrls,
                photoUrlMemoryCacheKey = state.photoUrlMemoryCacheKey,
              )
            )
            LazyColumn {
              item { Text(text = state.name, style = MaterialTheme.typography.displayLarge) }
              item { Text(text = state.description) }
            }
          }
        } else {
          LazyColumn(modifier = Modifier.padding(padding).testTag(ANIMAL_CONTAINER_TAG)) {
            item {
              CircuitContent(
                PetPhotoCarouselScreen(
                  name = state.name,
                  photoUrls = state.photoUrls,
                  photoUrlMemoryCacheKey = state.photoUrlMemoryCacheKey,
                )
              )
            }
            item { Text(text = state.name, style = MaterialTheme.typography.displayLarge) }
            item { Text(text = state.description) }
          }
        }
      }
    }
  }
}

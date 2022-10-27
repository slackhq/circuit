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
package com.slack.circuit.star.petdetail

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.star.R
import com.slack.circuit.star.data.Animal
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.repo.PetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize

@Parcelize
data class PetDetailScreen(val petId: Long, val photoUrlMemoryCacheKey: String?) : Screen {
  sealed interface State : CircuitUiState {
    object Loading : State

    object UnknownAnimal : State

    data class Success(
      val url: String,
      val photoUrls: List<String>,
      val photoUrlMemoryCacheKey: String?,
      val name: String,
      val description: String,
      val tags: List<String>,
    ) : State
  }
}

internal fun Animal.toPetDetailState(photoUrlMemoryCacheKey: String?): PetDetailScreen.State {
  return PetDetailScreen.State.Success(
    url = url,
    photoUrls = photos.map { it.large },
    photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
    name = name,
    description = description,
    tags =
      listOfNotNull(
        colors.primary,
        colors.secondary,
        breeds.primary,
        breeds.secondary,
        gender,
        size,
        status
      )
  )
}

class PetDetailPresenter
@AssistedInject
constructor(
  @Assisted private val screen: PetDetailScreen,
  private val petRepository: PetRepository,
) : Presenter<PetDetailScreen.State> {
  @Composable
  override fun present(): PetDetailScreen.State {
    val state by
      produceRetainedState<PetDetailScreen.State>(PetDetailScreen.State.Loading) {
        val animal = petRepository.getAnimal(screen.petId)
        value =
          when (animal) {
            null -> PetDetailScreen.State.UnknownAnimal
            else -> animal.toPetDetailState(screen.photoUrlMemoryCacheKey)
          }
      }

    return state
  }

  @CircuitInject(PetDetailScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(screen: PetDetailScreen): PetDetailPresenter
  }
}

internal object PetDetailTestConstants {
  const val ANIMAL_CONTAINER_TAG = "animal_container"
  const val PROGRESS_TAG = "progress"
  const val UNKNOWN_ANIMAL_TAG = "unknown_animal"
}

@CircuitInject(PetDetailScreen::class, AppScope::class)
@Composable
internal fun PetDetail(state: PetDetailScreen.State) {
  val systemUiController = rememberSystemUiController()
  systemUiController.setStatusBarColor(MaterialTheme.colorScheme.background)
  systemUiController.setNavigationBarColor(MaterialTheme.colorScheme.background)
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
            modifier = Modifier.padding(padding),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            CircuitContent(
              PetPhotoCarouselScreen(
                name = state.name,
                photoUrls = state.photoUrls,
                photoUrlMemoryCacheKey = state.photoUrlMemoryCacheKey,
              )
            )
            LazyColumn { petDetailDescriptions(state) }
          }
        } else {
          LazyColumn(
            modifier = Modifier.padding(padding).testTag(ANIMAL_CONTAINER_TAG),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            item {
              CircuitContent(
                PetPhotoCarouselScreen(
                  name = state.name,
                  photoUrls = state.photoUrls,
                  photoUrlMemoryCacheKey = state.photoUrlMemoryCacheKey,
                )
              )
            }
            petDetailDescriptions(state)
          }
        }
      }
    }
  }
}

private fun LazyListScope.petDetailDescriptions(state: PetDetailScreen.State.Success) {
  item(state.name) {
    Text(
      modifier = Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      text = state.name,
      style = MaterialTheme.typography.displayLarge
    )
  }
  item(state.tags) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      mainAxisSpacing = 8.dp,
      crossAxisSpacing = 8.dp,
      mainAxisAlignment = FlowMainAxisAlignment.Center,
      crossAxisAlignment = FlowCrossAxisAlignment.Center,
    ) {
      state.tags.forEach { tag ->
        Surface(
          color = MaterialTheme.colorScheme.tertiary,
          shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        ) {
          Text(
            modifier = Modifier.padding(12.dp),
            text = tag.capitalize(LocaleList.current),
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
  item(state.description) {
    Text(
      text = state.description.lineSequence().first(),
      style = MaterialTheme.typography.bodyLarge
    )
  }
  item(state.url) {
    val context = LocalContext.current
    Button(modifier = Modifier.fillMaxWidth(), onClick = { openTab(context, state.url) }) {
      Text(text = "Full bio on Petfinder ➡", style = MaterialTheme.typography.headlineSmall)
    }
  }
}

private fun openTab(context: Context, url: String) {
  val scheme = CustomTabColorSchemeParams.Builder().setToolbarColor(0x000000).build()
  CustomTabsIntent.Builder()
    .setColorSchemeParams(COLOR_SCHEME_LIGHT, scheme)
    .setColorSchemeParams(COLOR_SCHEME_DARK, scheme)
    .setShowTitle(true)
    .build()
    .launchUrl(context, Uri.parse(url))
}

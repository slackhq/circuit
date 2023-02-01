// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import android.content.res.Configuration
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.star.R
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.data.Animal
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.navigator.AndroidScreen
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.ui.ExpandableText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class PetDetailScreen(val petId: Long, val photoUrlMemoryCacheKey: String?) : Screen {
  sealed interface State : CircuitUiState {
    object Loading : State

    object UnknownAnimal : State

    data class Success(
      val url: String,
      val photoUrls: ImmutableList<String>,
      val photoUrlMemoryCacheKey: String?,
      val name: String,
      val description: String,
      val tags: ImmutableList<String>,
      val eventSink: (Event) -> Unit
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ViewFullBio(val url: String) : Event
  }
}

internal fun Animal.toPetDetailState(
  photoUrlMemoryCacheKey: String?,
  description: String = this.description,
  eventSink: (PetDetailScreen.Event) -> Unit
): PetDetailScreen.State {
  return PetDetailScreen.State.Success(
    url = url,
    photoUrls = photos.map { it.large }.toImmutableList(),
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
        .toImmutableList(),
    eventSink
  )
}

class PetDetailPresenter
@AssistedInject
constructor(
  @Assisted private val screen: PetDetailScreen,
  @Assisted private val navigator: Navigator,
  private val petRepository: PetRepository,
) : Presenter<PetDetailScreen.State> {
  @Composable
  override fun present(): PetDetailScreen.State {
    var title by remember { mutableStateOf<String?>(null) }
    val state by
      produceState<PetDetailScreen.State>(PetDetailScreen.State.Loading) {
        val animal = petRepository.getAnimal(screen.petId)
        val bioText = petRepository.getAnimalBio(screen.petId)
        value =
          when (animal) {
            null -> PetDetailScreen.State.UnknownAnimal
            else -> {
              title = animal.name
              animal.toPetDetailState(
                screen.photoUrlMemoryCacheKey,
                bioText ?: animal.description
              ) {
                navigator.goTo(AndroidScreen.CustomTabsIntentScreen(animal.url))
              }
            }
          }
      }

    return state
  }

  @CircuitInject(PetDetailScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(screen: PetDetailScreen, navigator: Navigator): PetDetailPresenter
  }
}

internal object PetDetailTestConstants {
  const val ANIMAL_CONTAINER_TAG = "animal_container"
  const val PROGRESS_TAG = "progress"
  const val UNKNOWN_ANIMAL_TAG = "unknown_animal"
  const val FULL_BIO_TAG = "full_bio"
}

@CircuitInject(PetDetailScreen::class, AppScope::class)
@Composable
internal fun PetDetail(state: PetDetailScreen.State, modifier: Modifier = Modifier) {
  val systemUiController = rememberSystemUiController()
  systemUiController.setStatusBarColor(MaterialTheme.colorScheme.background)
  systemUiController.setNavigationBarColor(MaterialTheme.colorScheme.background)

  Scaffold(modifier = modifier.systemBarsPadding(), topBar = { TopBar(state) }) { padding ->
    when (state) {
      is PetDetailScreen.State.Loading -> Loading(padding)
      is PetDetailScreen.State.UnknownAnimal -> UnknownAnimal(padding)
      is PetDetailScreen.State.Success -> ShowAnimal(state, padding)
    }
  }
}

@Composable
private fun TopBar(state: PetDetailScreen.State) {
  if (state !is PetDetailScreen.State.Success) return

  CenterAlignedTopAppBar(title = { Text(state.name) }, navigationIcon = { BackPressNavIcon() })
}

@Composable
private fun Loading(paddingValues: PaddingValues) {
  Box(
    modifier = Modifier.padding(paddingValues).fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(
      modifier = Modifier.testTag(PROGRESS_TAG),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun UnknownAnimal(paddingValues: PaddingValues) {
  Box(
    modifier = Modifier.padding(paddingValues).fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      modifier = Modifier.testTag(UNKNOWN_ANIMAL_TAG),
      text = stringResource(id = R.string.unknown_animals)
    )
  }
}

@Composable
private fun ShowAnimal(
  state: PetDetailScreen.State.Success,
  padding: PaddingValues,
) =
  when (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    true -> ShowAnimalLandscape(state, padding)
    false -> ShowAnimalPortrait(state, padding)
  }

@Composable
private fun ShowAnimalLandscape(state: PetDetailScreen.State.Success, padding: PaddingValues) {
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
}

@Composable
private fun ShowAnimalPortrait(state: PetDetailScreen.State.Success, padding: PaddingValues) {
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

private fun LazyListScope.petDetailDescriptions(state: PetDetailScreen.State.Success) {
  // Tags are ImmutableList and therefore cannot be a key since it's not Parcelable
  item(state.tags.hashCode()) {
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
    ExpandableText(text = state.description, style = MaterialTheme.typography.bodyLarge)
  }

  item(state.url) {
    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = { state.eventSink(PetDetailScreen.Event.ViewFullBio(state.url)) }
    ) {
      Text(
        modifier = Modifier.testTag(FULL_BIO_TAG),
        text = "Full bio on Petfinder ➡",
        style = MaterialTheme.typography.headlineSmall
      )
    }
  }
}

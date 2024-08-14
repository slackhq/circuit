// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.SharedElementTransitionScope
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.common.Platform
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.Assisted
import com.slack.circuit.star.di.AssistedFactory
import com.slack.circuit.star.di.AssistedInject
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.petdetail.PetDetailScreen.Event
import com.slack.circuit.star.petdetail.PetDetailScreen.Event.ViewFullBio
import com.slack.circuit.star.petdetail.PetDetailScreen.State
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Loading
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Success
import com.slack.circuit.star.petdetail.PetDetailScreen.State.UnknownAnimal
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.ui.ExpandableText
import kotlinx.collections.immutable.ImmutableList

@CommonParcelize
data class PetDetailScreen(val petId: Long, val photoUrlMemoryCacheKey: String?) : Screen {
  sealed interface State : CircuitUiState {
    data object Loading : State

    data object UnknownAnimal : State

    data class Success(
      val id: Long,
      val url: String,
      val photoUrls: ImmutableList<String>,
      val photoUrlMemoryCacheKey: String?,
      val name: String,
      val description: String,
      val tags: ImmutableList<String>,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ViewFullBio(val url: String) : Event
  }
}

internal fun Animal.toPetDetailState(
  photoUrlMemoryCacheKey: String?,
  description: String = this.description,
  eventSink: (Event) -> Unit,
): State {
  return Success(
    id = id,
    url = url,
    photoUrls = photoUrls,
    photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
    name = name,
    description = description,
    tags = tags,
    eventSink,
  )
}

class PetDetailPresenter
@AssistedInject
constructor(
  @Assisted private val screen: PetDetailScreen,
  @Assisted private val navigator: Navigator,
  private val petRepository: PetRepository,
) : Presenter<State> {
  @Composable
  override fun present(): State {
    var title by remember { mutableStateOf<String?>(null) }
    val state by
      produceState<State>(Loading) {
        val animal = petRepository.getAnimal(screen.petId)
        val bioText = petRepository.getAnimalBio(screen.petId)
        value =
          when (animal) {
            null -> UnknownAnimal
            else -> {
              title = animal.name
              animal.toPetDetailState(
                screen.photoUrlMemoryCacheKey,
                bioText ?: animal.description,
              ) {
                navigator.goTo(OpenUrlScreen(animal.url))
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
internal fun PetDetail(state: State, modifier: Modifier = Modifier) {
  Scaffold(modifier = modifier, topBar = { TopBar(state) }) { padding ->
    when (state) {
      is Loading -> Loading(padding)
      is UnknownAnimal -> UnknownAnimal(padding)
      is Success -> ShowAnimal(state, padding)
    }
  }
}

@Composable
private fun TopBar(state: State) {
  if (state !is Success) return
  CenterAlignedTopAppBar(title = { Text(state.name) }, navigationIcon = { BackPressNavIcon() })
}

@Composable
private fun Loading(paddingValues: PaddingValues) {
  Box(
    modifier = Modifier.padding(paddingValues).fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
      modifier = Modifier.testTag(PROGRESS_TAG),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun UnknownAnimal(paddingValues: PaddingValues) {
  Box(
    modifier = Modifier.padding(paddingValues).fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(modifier = Modifier.testTag(UNKNOWN_ANIMAL_TAG), text = Strings.UNKNOWN_ANIMALS)
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ShowAnimal(state: Success, padding: PaddingValues) = SharedElementTransitionScope {
  val sharedModifier =
    Modifier.padding(padding)
      .testTag(ANIMAL_CONTAINER_TAG)
      .sharedBounds(
        sharedContentState = rememberSharedContentState(key = "animal-${state.id}"),
        animatedVisibilityScope = requireAnimatedScope(Navigation),
      )
  val carouselContent = remember {
    movableContentOf {
      CircuitContent(
        PetPhotoCarouselScreen(
          id = state.id,
          name = state.name,
          photoUrls = state.photoUrls,
          photoUrlMemoryCacheKey = state.photoUrlMemoryCacheKey,
        )
      )
    }
  }
  when (Platform.isLandscape()) {
    true -> ShowAnimalLandscape(state, sharedModifier, carouselContent)
    false -> ShowAnimalPortrait(state, sharedModifier, carouselContent)
  }
}

@Composable
private fun ShowAnimalLandscape(
  state: Success,
  modifier: Modifier = Modifier,
  carouselContent: @Composable () -> Unit,
) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    carouselContent()
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
    ) {
      petDetailDescriptions(state)
    }
  }
}

@Composable
private fun ShowAnimalPortrait(
  state: Success,
  modifier: Modifier = Modifier,
  carouselContent: @Composable () -> Unit,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item { carouselContent() }
    petDetailDescriptions(state)
  }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.petDetailDescriptions(state: Success) {
  // Tags are ImmutableList and therefore cannot be a key since it's not Parcelable
  item(state.tags.hashCode()) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
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
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }

  item(state.description) {
    ExpandableText(
      text = state.description,
      style = MaterialTheme.typography.bodyLarge,
      initiallyExpanded = true,
    )
  }

  item(state.url) {
    Button(onClick = { state.eventSink(ViewFullBio(state.url)) }) {
      Text(
        modifier = Modifier.testTag(FULL_BIO_TAG),
        text = "Full bio on Petfinder ➡",
        style = MaterialTheme.typography.headlineSmall,
      )
    }
  }
}

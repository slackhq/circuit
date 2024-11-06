// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.slack.circuit.foundation.AnimatedScreen
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.thenIf
import com.slack.circuit.foundation.thenIfNotNull
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.sharedelements.DelicateCircuitSharedElementsApi
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.common.Platform
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.di.Assisted
import com.slack.circuit.star.di.AssistedFactory
import com.slack.circuit.star.di.AssistedInject
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.parcel.CommonParcelable
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.petdetail.PetDetailScreen.Event
import com.slack.circuit.star.petdetail.PetDetailScreen.Event.ViewFullBio
import com.slack.circuit.star.petdetail.PetDetailScreen.State
import com.slack.circuit.star.petdetail.PetDetailScreen.State.AnimalState
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Full
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Loading
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Partial
import com.slack.circuit.star.petdetail.PetDetailScreen.State.UnknownAnimal
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.transition.PetCardBoundsKey
import com.slack.circuit.star.transition.PetNameBoundsKey
import com.slack.circuit.star.ui.ExpandableText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

@CommonParcelize
data class PetDetailScreen(
  val petId: Long,
  val photoUrlMemoryCacheKey: String? = null,
  val animal: PartialAnimal? = null,
) : AnimatedScreen {

  override fun enterTransition(sharedElementTransition: Boolean) =
    if (sharedElementTransition) fadeIn() else null

  override fun exitTransition(sharedElementTransition: Boolean) =
    if (sharedElementTransition) fadeOut() else null

  override fun sharedElementTransitionKey(): Any? {
    return if (animal != null && photoUrlMemoryCacheKey != null) petId else null
  }

  @CommonParcelize
  data class PartialAnimal(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val breed: String?,
    val gender: Gender,
    val size: Size,
  ) : CommonParcelable

  sealed interface State : CircuitUiState {
    data object Loading : State

    data object UnknownAnimal : State

    sealed interface AnimalState : State {
      val id: Long
      val photoUrls: ImmutableList<String>
      val photoUrlMemoryCacheKey: String?
      val name: String
      val tags: ImmutableList<String>
    }

    data class Partial(
      override val id: Long,
      override val photoUrls: ImmutableList<String>,
      override val photoUrlMemoryCacheKey: String,
      override val name: String,
      override val tags: ImmutableList<String>,
    ) : AnimalState

    data class Full(
      override val id: Long,
      val url: String,
      override val photoUrls: ImmutableList<String>,
      override val photoUrlMemoryCacheKey: String?,
      override val name: String,
      val description: String,
      override val tags: ImmutableList<String>,
      val eventSink: (Event) -> Unit,
    ) : AnimalState
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
  return Full(
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

internal fun PetDetailScreen.toPetDetailState(): State {
  return if (animal != null && photoUrlMemoryCacheKey != null) {
    Partial(
      id = animal.id,
      photoUrls =
        persistentListOf<String>().mutate { list -> animal.imageUrl?.let { list.add(it) } },
      photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
      name = animal.name,
      tags =
        persistentListOf<String>().mutate { list ->
          animal.breed?.let { list.add(it) }
          list.add(animal.gender.displayName)
          list.add(animal.size.name.lowercase())
        },
    )
  } else Loading
}

class PetDetailPresenter
@AssistedInject
constructor(
  @Assisted private val screen: PetDetailScreen,
  @Assisted private val navigator: Navigator,
  private val petRepository: PetRepository,
) : Presenter<State> {

  private val initialState = screen.toPetDetailState()

  @Composable
  override fun present(): State {
    var title by remember { mutableStateOf<String?>(null) }
    val state by
      produceState<State>(initialState) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(PetDetailScreen::class, AppScope::class)
@Composable
internal fun PetDetail(state: State, modifier: Modifier = Modifier) = SharedElementTransitionScope {
  Scaffold(
    topBar = { TopBar(state) },
    modifier =
      modifier.thenIfNotNull((state as? AnimalState)?.id) { animalId ->
        sharedBounds(
          sharedContentState = rememberSharedContentState(key = PetCardBoundsKey(animalId)),
          animatedVisibilityScope = requireAnimatedScope(Navigation),
        )
      },
  ) { padding ->
    when (state) {
      is Loading -> Loading(padding)
      is UnknownAnimal -> UnknownAnimal(padding)
      is AnimalState -> ShowAnimal(state, padding)
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, DelicateCircuitSharedElementsApi::class)
@Composable
private fun TopBar(state: State) {
  if (state !is AnimalState) return
  SharedElementTransitionScope {
    CenterAlignedTopAppBar(
      title = {
        Text(
          state.name,
          modifier =
            Modifier.thenIf(hasLayoutCoordinates) {
              sharedBounds(
                sharedContentState = rememberSharedContentState(PetNameBoundsKey(state.id)),
                animatedVisibilityScope = requireAnimatedScope(Navigation),
                zIndexInOverlay = 3f,
              )
            },
        )
      },
      navigationIcon = { BackPressNavIcon() },
    )
  }
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

@Composable
private fun ShowAnimal(state: AnimalState, padding: PaddingValues) {
  val sharedModifier = Modifier.padding(padding).testTag(ANIMAL_CONTAINER_TAG)
  val carouselContent = remember {
    movableContentOf<AnimalState> {
      CircuitContent(
        screen =
          PetPhotoCarouselScreen(
            id = it.id,
            name = it.name,
            photoUrls = it.photoUrls,
            photoUrlMemoryCacheKey = it.photoUrlMemoryCacheKey,
          ),
        key = it.id,
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
  state: AnimalState,
  modifier: Modifier = Modifier,
  carouselContent: @Composable (AnimalState) -> Unit,
) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    carouselContent(state)
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
  state: AnimalState,
  modifier: Modifier = Modifier,
  carouselContent: @Composable (AnimalState) -> Unit,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item { carouselContent(state) }
    petDetailDescriptions(state)
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
private fun LazyListScope.petDetailDescriptions(state: AnimalState) {
  // Tags are ImmutableList and therefore cannot be a key since it's not Parcelable
  item(state.tags.hashCode()) {
    SharedElementTransitionScope {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
      ) {
        state.tags.forEach { tag ->
          Surface(
            color = MaterialTheme.colorScheme.tertiary,
            shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
            modifier =
              Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "tag-${state.id}-${tag}"),
                animatedVisibilityScope = requireAnimatedScope(Navigation),
                zIndexInOverlay = 2f,
              ),
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
  }

  when (state) {
    is Partial -> {
      item("partial-${state.id}") { Loading(PaddingValues(0.dp)) }
    }
    is Full -> {
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
  }
}

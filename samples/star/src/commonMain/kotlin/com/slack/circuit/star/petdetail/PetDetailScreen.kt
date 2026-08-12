// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.internal.runtime.Parcelable
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.serialization.CircuitSerializable
import com.slack.circuit.sharedelements.ExperimentalCircuitSharedElementsApi
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.star.common.BackPressNavIcon
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.common.isLandscape
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.PetAttribute
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.petdetail.PetDetailScreen.Event
import com.slack.circuit.star.petdetail.PetDetailScreen.Event.ViewFullBio
import com.slack.circuit.star.petdetail.PetDetailScreen.State
import com.slack.circuit.star.petdetail.PetDetailScreen.State.AnimalState
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Full
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Loading
import com.slack.circuit.star.petdetail.PetDetailScreen.State.Partial
import com.slack.circuit.star.petdetail.PetDetailScreen.State.UnknownAnimal
import com.slack.circuit.star.petdetail.PetDetailTestConstants.ANIMAL_CONTAINER_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.COMPACT_CLOSE_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.COMPACT_DETAILS_PANE_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.COMPACT_PHOTO_PANE_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.DESCRIPTION_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.FULL_BIO_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PET_NAME_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petdetail.PetDetailTestConstants.UNKNOWN_ANIMAL_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.transition.PetCardBoundsKey
import com.slack.circuit.star.transition.PetNameBoundsKey
import com.slack.circuit.star.ui.thenIf
import com.slack.circuit.star.ui.thenIfNotNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.serialization.Serializable

@Parcelize
@CircuitSerializable(AppScope::class)
data class PetDetailScreen(
  val petId: Long,
  val photoUrlMemoryCacheKey: String? = null,
  val animal: PartialAnimal? = null,
) : Screen {

  @Parcelize
  @Serializable
  data class PartialAnimal(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val breed: String?,
    val gender: Gender?,
    val size: Size?,
  ) : Parcelable

  sealed interface State : CircuitUiState {
    data object Loading : State

    data object UnknownAnimal : State

    sealed interface AnimalState : State {
      val id: Long
      val photoUrls: List<String>
      val photoUrlMemoryCacheKey: String?
      val photoAspectRatio: Float?
      val name: String
      val tags: List<String>
      val attributes: List<PetAttribute>
    }

    data class Partial(
      override val id: Long,
      override val photoUrls: List<String>,
      override val photoUrlMemoryCacheKey: String,
      override val photoAspectRatio: Float?,
      override val name: String,
      override val tags: List<String>,
      override val attributes: List<PetAttribute> = emptyList(),
    ) : AnimalState

    data class Full(
      override val id: Long,
      val url: String,
      override val photoUrls: List<String>,
      override val photoUrlMemoryCacheKey: String?,
      override val photoAspectRatio: Float?,
      override val name: String,
      val descriptionMarkdown: String?,
      override val tags: List<String>,
      override val attributes: List<PetAttribute>,
      val eventSink: (Event) -> Unit,
    ) : AnimalState
  }

  sealed interface Event : CircuitUiEvent {
    data class ViewFullBio(val url: String) : Event
  }
}

internal fun Animal.toPetDetailState(
  photoUrlMemoryCacheKey: String?,
  eventSink: (Event) -> Unit,
): State {
  return Full(
    id = id,
    url = url,
    photoUrls = photos.map { it.originalUrl },
    photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
    photoAspectRatio = photos.firstOrNull()?.aspectRatio,
    name = name,
    descriptionMarkdown = descriptionMarkdown?.normalizePetDescriptionMarkdown(),
    tags = tags,
    attributes = attributes,
    eventSink,
  )
}

internal fun String.normalizePetDescriptionMarkdown(): String {
  // HTML strong tags sometimes include an opening quote, producing invalid Markdown delimiters.
  return MALFORMED_BOLD_LABEL.replace(this) { match ->
    "**${match.groupValues[1]}:** ${match.groupValues[2]}"
  }
}

private val MALFORMED_BOLD_LABEL = Regex("""\*\*([^*\n]+): ([\"“])\*\*(?=[\p{L}\p{N}])""")

internal fun PetDetailScreen.toPetDetailState(): State {
  return if (animal != null && photoUrlMemoryCacheKey != null) {
    Partial(
      id = animal.id,
      photoUrls = buildList { animal.imageUrl?.let { add(it) } },
      photoUrlMemoryCacheKey = photoUrlMemoryCacheKey,
      // Partial state doesn't have aspect ratio, will be loaded with full state
      photoAspectRatio = null,
      name = animal.name,
      tags =
        buildList {
          animal.breed?.let { add(it) }
          animal.gender?.let { add(it.displayName) }
          animal.size?.let { add(it.name.lowercase()) }
        },
    )
  } else Loading
}

@AssistedInject
class PetDetailPresenter(
  @Assisted private val screen: PetDetailScreen,
  @Assisted private val navigator: Navigator,
  private val petRepository: PetRepository,
) : Presenter<State> {

  private val initialState = screen.toPetDetailState()

  @Composable
  override fun present(): State {
    val state by
      produceState(initialState) {
        val animal = petRepository.getAnimal(screen.petId)
        value =
          when (animal) {
            null -> UnknownAnimal
            else -> {
              animal.toPetDetailState(screen.photoUrlMemoryCacheKey) {
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
  const val COMPACT_CLOSE_TAG = "compact_close"
  const val COMPACT_DETAILS_PANE_TAG = "compact_details_pane"
  const val COMPACT_PHOTO_PANE_TAG = "compact_photo_pane"
  const val DESCRIPTION_TAG = "description"
  const val PROGRESS_TAG = "progress"
  const val UNKNOWN_ANIMAL_TAG = "unknown_animal"
  const val FULL_BIO_TAG = "full_bio"
  const val PET_NAME_TAG = "pet_name"
}

@OptIn(
  ExperimentalMaterial3WindowSizeClassApi::class,
  ExperimentalSharedTransitionApi::class,
)
@CircuitInject(PetDetailScreen::class, AppScope::class)
@Composable
internal fun PetDetail(state: State, modifier: Modifier = Modifier) = SharedElementTransitionScope {
  val useCompactLandscapeLayout =
    isLandscape() && calculateWindowSizeClass().heightSizeClass == WindowHeightSizeClass.Compact
  Scaffold(
    topBar = { if (!useCompactLandscapeLayout) TopBar(state) },
    modifier =
      modifier.thenIfNotNull((state as? AnimalState)?.id) { animalId ->
        sharedBounds(
          sharedContentState = rememberSharedContentState(key = PetCardBoundsKey(animalId)),
          animatedVisibilityScope = requireAnimatedScope(Navigation),
          enter = fadeIn(tween(easing = EaseOutCubic)),
          exit = fadeOut(tween(easing = EaseOutCubic)),
        )
      },
  ) { padding ->
    when (state) {
      is Loading -> Loading(padding)
      is UnknownAnimal -> UnknownAnimal(padding)
      is AnimalState -> ShowAnimal(state, padding, useCompactLandscapeLayout)
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCircuitSharedElementsApi::class)
@Composable
private fun TopBar(state: State) {
  if (state !is AnimalState) return
  CenterAlignedTopAppBar(
    title = { PetName(state, style = MaterialTheme.typography.titleLarge) },
    navigationIcon = { BackPressNavIcon() },
  )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCircuitSharedElementsApi::class)
@Composable
private fun PetName(state: AnimalState, style: TextStyle, modifier: Modifier = Modifier) {
  SharedElementTransitionScope {
    Text(
      state.name,
      style = style,
      modifier =
        modifier.testTag(PET_NAME_TAG).thenIf(hasLayoutCoordinates) {
          sharedBounds(
            sharedContentState = rememberSharedContentState(PetNameBoundsKey(state.id)),
            animatedVisibilityScope = requireAnimatedScope(Navigation),
            zIndexInOverlay = 2f,
          )
        },
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
private fun ShowAnimal(
  state: AnimalState,
  scaffoldPadding: PaddingValues,
  useCompactLandscapeLayout: Boolean,
) {
  val carouselContent = remember {
    movableContentOf<AnimalState> {
      CircuitContent(
        screen =
          PetPhotoCarouselScreen(
            id = it.id,
            name = it.name,
            photoUrls = it.photoUrls,
            photoUrlMemoryCacheKey = it.photoUrlMemoryCacheKey,
            photoAspectRatio = it.photoAspectRatio,
          ),
        key = it.id,
      )
    }
  }
  when {
    useCompactLandscapeLayout -> ShowAnimalCompactLandscape(state, scaffoldPadding, carouselContent)
    isLandscape() -> ShowAnimalLandscape(state, scaffoldPadding, carouselContent)
    else -> ShowAnimalPortrait(state, scaffoldPadding, carouselContent)
  }
}

@Composable
private fun ShowAnimalCompactLandscape(
  state: AnimalState,
  scaffoldPadding: PaddingValues,
  carouselContent: @Composable (AnimalState) -> Unit,
) {
  Row(
    modifier =
      Modifier.padding(scaffoldPadding)
        .padding(start = 8.dp, end = 16.dp, bottom = 8.dp)
        .fillMaxSize()
        .testTag(ANIMAL_CONTAINER_TAG),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Box(
      modifier = Modifier.weight(2f).fillMaxHeight().testTag(COMPACT_PHOTO_PANE_TAG),
      contentAlignment = Alignment.Center,
    ) {
      carouselContent(state)
      Surface(
        modifier = Modifier.align(Alignment.TopStart).padding(4.dp).testTag(COMPACT_CLOSE_TAG),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 2.dp,
      ) {
        BackPressNavIcon()
      }
    }
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.Start,
      modifier =
        Modifier.weight(3f)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState())
          .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
          .testTag(COMPACT_DETAILS_PANE_TAG),
    ) {
      PetName(
        state = state,
        style = MaterialTheme.typography.headlineMedium,
      )
      PetDetailDescriptions(state, horizontalAlignment = Alignment.Start)
    }
  }
}

@Composable
private fun ShowAnimalLandscape(
  state: AnimalState,
  scaffoldPadding: PaddingValues,
  carouselContent: @Composable (AnimalState) -> Unit,
) {
  Row(
    modifier = Modifier.padding(scaffoldPadding).testTag(ANIMAL_CONTAINER_TAG),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      carouselContent(state)
    }
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
    ) {
      PetDetailDescriptions(state)
    }
  }
}

@Composable
private fun ShowAnimalPortrait(
  state: AnimalState,
  scaffoldPadding: PaddingValues,
  carouselContent: @Composable (AnimalState) -> Unit,
) {
  Column(
    modifier =
      Modifier.testTag(ANIMAL_CONTAINER_TAG)
        .verticalScroll(rememberScrollState())
        .padding(
          start = 16.dp,
          end = 16.dp,
          top = scaffoldPadding.calculateTopPadding() + 16.dp,
          bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
        ),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    carouselContent(state)
    PetDetailDescriptions(state)
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PetDetailDescriptions(
  state: AnimalState,
  horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
  SharedElementTransitionScope {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, horizontalAlignment),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = tag.capitalize(LocaleList.current),
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
      // Render attributes as secondary-colored tags
      state.attributes.forEach { attribute ->
        Surface(
          color = MaterialTheme.colorScheme.secondary,
          shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        ) {
          Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = attribute.display,
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }

  if (state.tags.isNotEmpty() || state.attributes.isNotEmpty()) {
    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
  }

  when (state) {
    is Partial -> {
      Loading(PaddingValues(0.dp))
    }
    is Full -> {
      if (!state.descriptionMarkdown.isNullOrBlank()) {
        Markdown(
          content = state.descriptionMarkdown,
          typography = petDescriptionMarkdownTypography(),
          modifier = Modifier.fillMaxWidth().testTag(DESCRIPTION_TAG),
        )
      }
      Button(
        onClick = { state.eventSink(ViewFullBio(state.url)) },
        modifier = Modifier.testTag(FULL_BIO_TAG),
      ) {
        Text(
          text = "View full bio on Adopt a Pet",
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
internal fun petDescriptionMarkdownTypography(): MarkdownTypography {
  val body = MaterialTheme.typography.bodyMedium
  return markdownTypography(
    h1 = MaterialTheme.typography.headlineSmall,
    h2 = MaterialTheme.typography.titleLarge,
    h3 = MaterialTheme.typography.titleMedium,
    h4 = MaterialTheme.typography.titleSmall,
    h5 = MaterialTheme.typography.labelLarge,
    h6 = MaterialTheme.typography.labelMedium,
    text = body,
    paragraph = body,
    ordered = body,
    bullet = body,
    list = body,
    textLink =
      TextLinkStyles(
        style =
          body
            .copy(
              color = MaterialTheme.colorScheme.secondary,
              textDecoration = TextDecoration.Underline,
            )
            .toSpanStyle()
      ),
  )
}

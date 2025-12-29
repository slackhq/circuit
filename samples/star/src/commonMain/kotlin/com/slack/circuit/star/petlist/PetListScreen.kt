// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.animatedSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest.Builder
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.rememberAnsweringNavigator
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.overlay.OverlayEffect
import com.slack.circuit.retained.collectAsRetainedState
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.progress
import com.slack.circuit.star.common.Strings
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petlist.PetListScreen.Event
import com.slack.circuit.star.petlist.PetListScreen.Event.ClickAnimal
import com.slack.circuit.star.petlist.PetListScreen.Event.GoToFiltersScreen
import com.slack.circuit.star.petlist.PetListScreen.Event.Refresh
import com.slack.circuit.star.petlist.PetListScreen.Event.ShowFiltersOverlay
import com.slack.circuit.star.petlist.PetListScreen.Event.UpdatedFilters
import com.slack.circuit.star.petlist.PetListScreen.State
import com.slack.circuit.star.petlist.PetListScreen.State.Loading
import com.slack.circuit.star.petlist.PetListScreen.State.NoAnimals
import com.slack.circuit.star.petlist.PetListScreen.State.Success
import com.slack.circuit.star.petlist.PetListTestConstants.AGE_AND_BREED_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.transition.PetCardBoundsKey
import com.slack.circuit.star.transition.PetImageBoundsKey
import com.slack.circuit.star.transition.PetNameBoundsKey
import com.slack.circuit.star.ui.FilterList
import com.slack.circuit.star.ui.Pets
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.ktor.util.Platform
import io.ktor.util.PlatformUtils
import io.ktor.util.platform
import kotlinx.coroutines.flow.map

@Parcelize
data object PetListScreen : Screen {

  sealed interface State : CircuitUiState {
    val isRefreshing: Boolean

    data object Loading : State {
      override val isRefreshing: Boolean = false
    }

    data class NoAnimals(override val isRefreshing: Boolean) : State

    data class Success(
      val animals: List<PetListAnimal>,
      override val isRefreshing: Boolean,
      val filters: Filters = Filters(),
      val isUpdateFiltersModalShowing: Boolean = false,
      val eventSink: (Event) -> Unit = {},
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ClickAnimal(
      val petId: Long,
      val photoUrlMemoryCacheKey: String?,
      val animal: PetListAnimal,
    ) : Event

    data object Refresh : Event

    data object ShowFiltersOverlay : Event

    data object GoToFiltersScreen : Event

    data class UpdatedFilters(val newFilters: Filters) : Event
  }
}

@AssistedInject
class PetListPresenter(
  @Assisted private val navigator: Navigator,
  private val petRepo: PetRepository,
) : Presenter<State> {
  @Composable
  override fun present(): State {
    var isRefreshing by rememberRetained { mutableStateOf(false) }
    if (isRefreshing) {
      LaunchedEffect(Unit) {
        petRepo.refreshData()
        isRefreshing = false
      }
    }

    val animalsFlow =
      rememberRetained(petRepo) {
        petRepo.animalsFlow().map { animals -> animals?.map(Animal::toPetListAnimal) }
      }

    val animalState by animalsFlow.collectAsRetainedState(null)

    var isUpdateFiltersModalShowing by rememberRetained { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(Filters()) }
    val filteredAnimals by
      rememberRetained(animalState, filters) {
        derivedStateOf { animalState?.filter { shouldKeep(filters, it) }?.toList() }
      }

    val filtersScreenNavigator =
      rememberAnsweringNavigator<FiltersScreen.Result>(navigator) { filters = it.filters }

    val animals = filteredAnimals
    return when {
      animals == null -> Loading
      animals.isEmpty() -> NoAnimals(isRefreshing)
      else ->
        Success(
          animals = animals,
          isRefreshing = isRefreshing,
          filters = filters,
          isUpdateFiltersModalShowing = isUpdateFiltersModalShowing,
        ) { event ->
          when (event) {
            is ClickAnimal -> {
              navigator.goTo(
                screen =
                  PetDetailScreen(
                    event.petId,
                    event.photoUrlMemoryCacheKey,
                    event.animal.toPartialAnimal(),
                  )
              )
            }
            is UpdatedFilters -> {
              isUpdateFiltersModalShowing = false
              filters = event.newFilters
            }
            ShowFiltersOverlay -> {
              isUpdateFiltersModalShowing = true
            }
            GoToFiltersScreen -> {
              filtersScreenNavigator.goTo(FiltersScreen(filters))
            }
            Refresh -> isRefreshing = true
          }
        }
    }
  }

  private fun shouldKeep(filters: Filters, animal: PetListAnimal): Boolean {
    // Null gender/size means unknown - always include unless filters explicitly exclude
    val genderMatch = animal.gender == null || animal.gender in filters.genders
    val sizeMatch = animal.size == null || animal.size in filters.sizes
    return genderMatch && sizeMatch
  }

  @CircuitInject(PetListScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): PetListPresenter
  }
}

internal fun Animal.toPetListAnimal(): PetListAnimal {
  val primaryPhoto = photos.firstOrNull()
  return PetListAnimal(
    id = id,
    name = name,
    imageUrl = primaryPhoto?.originalUrl?.toThumbnailUrl(),
    breed = primaryBreed,
    gender = gender,
    size = size,
    age = age,
    imageAspectRatio = primaryPhoto?.aspectRatio,
  )
}

/**
 * Convert a Cloudinary URL to a thumbnail version by replacing the transformation params. This
 * extracts the image ID and creates a smaller, aspect-ratio-preserving thumbnail.
 */
private fun String.toThumbnailUrl(): String {
  // Extract image ID from URL like:
  // https://media.adoptapet.com/image/upload/c_fill,w_800,h_600,g_auto/f_auto,q_auto/1268757503
  val imageId = substringAfterLast('/').takeIf { it.isNotBlank() } ?: return this
  // Create a thumbnail that scales to 400px wide while preserving aspect ratio
  return "https://media.adoptapet.com/image/upload/c_scale,w_400/f_auto,q_auto/$imageId"
}

internal fun PetListAnimal.toPartialAnimal(): PetDetailScreen.PartialAnimal {
  return PetDetailScreen.PartialAnimal(
    id = id,
    name = name,
    imageUrl = imageUrl,
    breed = breed,
    gender = gender,
    size = size,
  )
}

internal object PetListTestConstants {
  const val PROGRESS_TAG = "progress"
  const val NO_ANIMALS_TAG = "no_animals"
  const val GRID_TAG = "grid"
  const val CARD_TAG = "card"
  const val IMAGE_TAG = "image"
  const val AGE_AND_BREED_TAG = "age_and_breed"
}

@CircuitInject(PetListScreen::class, AppScope::class)
@Composable
internal fun PetList(state: State, modifier: Modifier = Modifier) {
  if (state is Success && state.isUpdateFiltersModalShowing) {
    OverlayEffect(state) {
      val result = updateFilters(state.filters)
      state.eventSink(UpdatedFilters(result))
    }
  }

  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        scrollBehavior = scrollBehavior,
        actions = {
          if (state is Success) {
            CompositeIconButton(
              onClick = { state.eventSink(ShowFiltersOverlay) },
              onLongClick = { state.eventSink(GoToFiltersScreen) },
            ) {
              Icon(
                imageVector = FilterList,
                contentDescription = "Filter pet list",
                tint = MaterialTheme.colorScheme.onBackground,
              )
            }
          }
        },
      )
    },
  ) { paddingValues ->
    when (state) {
      Loading ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            modifier = Modifier.testTag(PROGRESS_TAG),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      is NoAnimals ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(modifier = Modifier.testTag(NO_ANIMALS_TAG), text = Strings.NO_ANIMALS)
        }
      is Success ->
        PetListGrid(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          animals = state.animals,
          isRefreshing = state.isRefreshing,
          eventSink = state.eventSink,
        )
    }
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PetListGrid(
  animals: List<PetListAnimal>,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  eventSink: (Event) -> Unit,
) {
  val pullRefreshState =
    rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { eventSink(Refresh) })

  val focusRequester = remember { FocusRequester() }
  Box(
    modifier =
      modifier
        .pullRefresh(pullRefreshState)
        .focusRequester(focusRequester)
        // Keyboard shortcut for refresh calls
        .onKeyEvent { event ->
          if (event.key == Key.R && event.isMetaPressed && event.type == KeyEventType.KeyDown) {
            eventSink(Refresh)
            true
          } else {
            false
          }
        }
  ) {
    // For the refresh keyboard shortcut
    if (PlatformUtils.platform == Platform.Jvm) {
      LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    @Suppress("MagicNumber")
    val columnSpan =
      when (calculateWindowSizeClass().widthSizeClass) {
        WindowWidthSizeClass.Medium -> 3
        WindowWidthSizeClass.Expanded -> 4
        // No exhaustive whens available here
        else -> 2
      }

    val spacing = if (columnSpan >= 4) 32.dp else 16.dp
    @Suppress("MagicNumber")
    LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Fixed(columnSpan),
      modifier = Modifier.fillMaxSize().testTag(GRID_TAG),
      verticalItemSpacing = spacing,
      horizontalArrangement = spacedBy(spacing),
      contentPadding = PaddingValues(spacing),
    ) {
      items(count = animals.size, key = { i -> animals[i].id }) { index ->
        val animal = animals[index]
        PetListGridItem(animal, modifier = Modifier.animateItem()) {
          eventSink(ClickAnimal(animal.id, animal.imageUrl, animal))
        }
      }
    }
    PullRefreshIndicator(
      modifier = Modifier.align(Alignment.TopCenter),
      refreshing = isRefreshing,
      state = pullRefreshState,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PetListGridItem(
  animal: PetListAnimal,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) = SharedElementTransitionScope {
  val animatedScope = requireAnimatedScope(Navigation)
  val boundsState = rememberSharedContentState(key = PetCardBoundsKey(animal.id))
  val fraction by
    remember(boundsState, animatedScope) {
      derivedStateOf { if (boundsState.isMatchFound) animatedScope.progress().value else 1f }
    }
  val topCornerSize = lerp(12.dp, 16.dp, fraction)
  val bottomCornerSize = lerp(0.dp, 12.dp, 1 - fraction)
  ElevatedCard(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag(CARD_TAG)
        .sharedBounds(
          sharedContentState = boundsState,
          animatedVisibilityScope = animatedScope,
          enter = fadeIn(tween(durationMillis = 100, easing = EaseOutCubic)),
          exit = fadeOut(tween(durationMillis = 40, easing = EaseInCubic)),
          zIndexInOverlay = 1f,
        ),
    shape = RoundedCornerShape(topCornerSize),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
  ) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
      // Image
      val imageModifier =
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = PetImageBoundsKey(animal.id, 0)),
            animatedVisibilityScope = animatedScope,
            placeHolderSize = animatedSize,
            resizeMode = RemeasureToBounds,
            enter = fadeIn(animationSpec = tween(durationMillis = 80, easing = EaseInExpo)),
            exit = fadeOut(animationSpec = tween(durationMillis = 80, easing = EaseOutExpo)),
            zIndexInOverlay = 3f,
          )
          .clip(
            RoundedCornerShape(
              topStart = topCornerSize,
              topEnd = topCornerSize,
              bottomStart = bottomCornerSize,
              bottomEnd = bottomCornerSize,
            )
          )
          .fillMaxWidth()
          .testTag(IMAGE_TAG)
      // Use the image's aspect ratio if available
      val imageAspectRatio = animal.imageAspectRatio
      if (animal.imageUrl == null) {
        Image(
          rememberVectorPainter(Pets),
          modifier =
            imageModifier
              .let {
                if (imageAspectRatio == null) {
                  it
                } else {
                  it.aspectRatio(imageAspectRatio)
                }
              }
              .padding(8.dp),
          contentDescription = animal.name,
          contentScale = ContentScale.Fit,
          colorFilter = ColorFilter.tint(LocalContentColor.current),
        )
      } else {
        AsyncImage(
          modifier =
            imageModifier.let {
              if (imageAspectRatio == null) {
                it
              } else {
                it.aspectRatio(imageAspectRatio)
              }
            },
          model =
            Builder(LocalPlatformContext.current)
              .data(animal.imageUrl)
              .memoryCacheKey(animal.imageUrl)
              .crossfade(AnimationConstants.DefaultDurationMillis)
              .build(),
          contentDescription = animal.name,
          contentScale = ContentScale.Fit,
          imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
        )
      }
      Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        // Name
        Text(
          text = animal.name,
          style = MaterialTheme.typography.labelLarge,
          modifier =
            Modifier.sharedBounds(
              sharedContentState = rememberSharedContentState(PetNameBoundsKey(animal.id)),
              animatedVisibilityScope = requireAnimatedScope(Navigation),
              zIndexInOverlay = 2f,
            ),
        )
        // Type
        animal.breed?.let {
          Text(
            text = animal.breed,
            style = MaterialTheme.typography.bodyMedium,
            modifier =
              Modifier.sharedBounds(
                sharedContentState =
                  rememberSharedContentState(key = "tag-${animal.id}-${animal.breed}"),
                animatedVisibilityScope = requireAnimatedScope(Navigation),
                zIndexInOverlay = 2f,
              ),
          )
        }
        CompositionLocalProvider(
          LocalContentColor provides LocalContentColor.current.copy(alpha = 0.75f)
        ) {
          // Gender, age
          val genderAge =
            buildList {
                animal.gender?.let { add(it.displayName) }
                add(animal.age)
              }
              .joinToString(" – ")
          Text(
            modifier = Modifier.testTag(AGE_AND_BREED_TAG),
            text = genderAge,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
  }
}

@Composable
internal fun UpdateFiltersSheet(
  initialFilters: Filters,
  modifier: Modifier = Modifier,
  onDismiss: (Filters) -> Unit = {},
) {
  Column(modifier.fillMaxWidth(), verticalArrangement = spacedBy(16.dp)) {
    val genderOptions = remember {
      SnapshotStateMap<Gender, Boolean>().apply {
        for (gender in Gender.entries) {
          put(gender, gender in initialFilters.genders)
        }
      }
    }
    FilterOptions("Gender", genderOptions)

    val sizeOptions = remember {
      SnapshotStateMap<Size, Boolean>().apply {
        for (size in Size.entries) {
          put(size, size in initialFilters.sizes)
        }
      }
    }
    FilterOptions("Size", sizeOptions)

    Row(Modifier.align(Alignment.End)) {
      Button(onClick = { onDismiss(initialFilters) }) { Text("Cancel") }
      Spacer(Modifier.width(16.dp))
      val saveButtonEnabled by remember {
        derivedStateOf { sizeOptions.values.any { it } && genderOptions.values.any { it } }
      }
      Button(
        enabled = saveButtonEnabled,
        onClick = {
          val newFilters =
            Filters(
              genders = genderOptions.filterValues { it }.keys,
              sizes = sizeOptions.filterValues { it }.keys,
            )
          onDismiss(newFilters)
        },
      ) {
        Text("Save")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Enum<T>> FilterOptions(
  name: String,
  options: SnapshotStateMap<T, Boolean>,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Text(name, style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = spacedBy(8.dp)) {
      options.keys
        .sortedBy { it.ordinal }
        .forEach { key ->
          val selected = options.getValue(key)
          val leadingIcon: @Composable () -> Unit = { Icon(Icons.Default.Check, null) }
          FilterChip(
            selected,
            onClick = { options[key] = !selected },
            label = { Text(key.name.lowercase().capitalize(Locale.current)) },
            leadingIcon = if (selected) leadingIcon else null,
          )
        }
    }
  }
}

/** A very simple alternative to `IconButton` that supports [onLongClick] too. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompositeIconButton(
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .size(40.dp)
        .clip(MaterialTheme.shapes.small)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Button),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

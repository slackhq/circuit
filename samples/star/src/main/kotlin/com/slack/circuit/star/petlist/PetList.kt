// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import android.content.res.Configuration
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.R
import com.slack.circuit.star.common.ImmutableSetParceler
import com.slack.circuit.star.db.Animal
import com.slack.circuit.star.db.Gender
import com.slack.circuit.star.db.Size
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petlist.PetListTestConstants.AGE_AND_BREED_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.star.ui.LocalWindowWidthSizeClass
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.overlays.BottomSheetOverlay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Immutable
data class PetListAnimal(
  val id: Long,
  val name: String,
  val imageUrl: String?,
  val breed: String?,
  val gender: Gender,
  val size: Size,
  val age: String,
)

@Parcelize
class Filters(
  @TypeParceler<ImmutableSet<Gender>, ImmutableSetParceler>
  val genders: ImmutableSet<Gender> = Gender.values().asIterable().toImmutableSet(),
  @TypeParceler<ImmutableSet<Size>, ImmutableSetParceler>
  val sizes: ImmutableSet<Size> = Size.values().asIterable().toImmutableSet()
) : Parcelable

@Parcelize
data object PetListScreen : Screen {
  sealed interface State : CircuitUiState {
    val isRefreshing: Boolean

    data object Loading : State {
      override val isRefreshing: Boolean = false
    }

    data class NoAnimals(override val isRefreshing: Boolean) : State

    data class Success(
      val animals: ImmutableList<PetListAnimal>,
      override val isRefreshing: Boolean,
      val filters: Filters = Filters(),
      val isUpdateFiltersModalShowing: Boolean = false,
      val eventSink: (Event) -> Unit = {},
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ClickAnimal(val petId: Long, val photoUrlMemoryCacheKey: String?) : Event

    data object Refresh : Event

    data object UpdateFilters : Event

    data class UpdatedFilters(val newFilters: Filters) : Event
  }
}

class PetListPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val petRepo: PetRepository,
) : Presenter<PetListScreen.State> {
  @Composable
  override fun present(): PetListScreen.State {
    var isRefreshing by remember { mutableStateOf(false) }
    if (isRefreshing) {
      LaunchedEffect(Unit) {
        petRepo.refreshData()
        isRefreshing = false
      }
    }

    val animalState by
      produceState<List<PetListAnimal>?>(initialValue = null, petRepo) {
        petRepo
          .animalsFlow()
          .map { animals -> animals?.map(Animal::toPetListAnimal) }
          .collect { value = it }
      }

    var isUpdateFiltersModalShowing by rememberSaveable { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(Filters()) }

    val animals = animalState
    return when {
      animals == null -> PetListScreen.State.Loading
      animals.isEmpty() -> PetListScreen.State.NoAnimals(isRefreshing)
      else ->
        PetListScreen.State.Success(
          animals = animals.filter { shouldKeep(filters, it) }.toImmutableList(),
          isRefreshing = isRefreshing,
          filters = filters,
          isUpdateFiltersModalShowing = isUpdateFiltersModalShowing,
        ) { event ->
          when (event) {
            is PetListScreen.Event.ClickAnimal -> {
              navigator.goTo(PetDetailScreen(event.petId, event.photoUrlMemoryCacheKey))
            }
            is PetListScreen.Event.UpdatedFilters -> {
              isUpdateFiltersModalShowing = false
              filters = event.newFilters
            }
            PetListScreen.Event.UpdateFilters -> {
              isUpdateFiltersModalShowing = true
            }
            PetListScreen.Event.Refresh -> isRefreshing = true
          }
        }
    }
  }

  private fun shouldKeep(filters: Filters, animal: PetListAnimal): Boolean {
    return animal.gender in filters.genders && animal.size in filters.sizes
  }

  @CircuitInject(PetListScreen::class, AppScope::class)
  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): PetListPresenter
  }
}

internal fun Animal.toPetListAnimal(): PetListAnimal {
  return PetListAnimal(
    id = id,
    name = name,
    imageUrl = primaryPhotoUrl,
    breed = primaryBreed,
    gender = gender,
    size = size,
    age = age
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
internal fun PetList(
  state: PetListScreen.State,
  modifier: Modifier = Modifier,
) {
  if (state is PetListScreen.State.Success && state.isUpdateFiltersModalShowing) {
    val overlayHost = LocalOverlayHost.current
    LaunchedEffect(state) {
      val result = overlayHost.updateFilters(state.filters)
      state.eventSink(PetListScreen.Event.UpdatedFilters(result))
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
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
        scrollBehavior = scrollBehavior,
        actions = {
          if (state is PetListScreen.State.Success) {
            IconButton(onClick = { state.eventSink(PetListScreen.Event.UpdateFilters) }) {
              Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter pet list",
                tint = MaterialTheme.colorScheme.onBackground
              )
            }
          }
        },
      )
    },
  ) { paddingValues ->
    when (state) {
      PetListScreen.State.Loading ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            modifier = Modifier.testTag(PROGRESS_TAG),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      is PetListScreen.State.NoAnimals ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            modifier = Modifier.testTag(NO_ANIMALS_TAG),
            text = stringResource(id = R.string.no_animals)
          )
        }
      is PetListScreen.State.Success ->
        PetListGrid(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          animals = state.animals,
          isRefreshing = state.isRefreshing,
          eventSink = state.eventSink
        )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PetListGrid(
  animals: ImmutableList<PetListAnimal>,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  eventSink: (PetListScreen.Event) -> Unit,
) {
  val pullRefreshState =
    rememberPullRefreshState(
      refreshing = isRefreshing,
      onRefresh = { eventSink(PetListScreen.Event.Refresh) }
    )
  Box(modifier = modifier.pullRefresh(pullRefreshState)) {
    @Suppress("MagicNumber")
    val columnSpan =
      when (LocalWindowWidthSizeClass.current) {
        WindowWidthSizeClass.Medium -> 3
        WindowWidthSizeClass.Expanded -> 4
        // No exhaustive whens available here
        else -> 2
      }

    @Suppress("MagicNumber")
    LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Fixed(columnSpan),
      modifier = Modifier.fillMaxSize().testTag(GRID_TAG),
      verticalItemSpacing = 16.dp,
      horizontalArrangement = spacedBy(16.dp),
      contentPadding = PaddingValues(16.dp),
    ) {
      items(
        count = animals.size,
        key = { i -> animals[i].id },
      ) { index ->
        val animal = animals[index]
        // TODO eventually animate item placement once it's implemented
        //  https://issuetracker.google.com/issues/257034719
        PetListGridItem(animal) {
          eventSink(PetListScreen.Event.ClickAnimal(animal.id, animal.imageUrl))
        }
      }
    }
    PullRefreshIndicator(
      modifier = Modifier.align(Alignment.TopCenter),
      refreshing = isRefreshing,
      state = pullRefreshState
    )
  }
}

@Composable
private fun PetListGridItem(
  animal: PetListAnimal,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {}
) {
  val updatedImageUrl = animal.imageUrl ?: R.drawable.star_icon
  ElevatedCard(
    modifier = modifier.fillMaxWidth().testTag(CARD_TAG),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
  ) {
    Column(modifier = Modifier.clickable { onClick() }) {
      // Image
      AsyncImage(
        modifier = Modifier.fillMaxWidth().testTag(IMAGE_TAG),
        model =
          ImageRequest.Builder(LocalContext.current)
            .data(updatedImageUrl)
            .memoryCacheKey(animal.imageUrl)
            .crossfade(AnimationConstants.DefaultDurationMillis)
            .build(),
        contentDescription = animal.name,
        contentScale = ContentScale.Crop,
      )
      Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        // Name
        Text(text = animal.name, style = MaterialTheme.typography.labelLarge)
        // Type
        animal.breed?.let { Text(text = animal.breed, style = MaterialTheme.typography.bodyMedium) }
        CompositionLocalProvider(
          LocalContentColor provides LocalContentColor.current.copy(alpha = 0.75f)
        ) {
          // Gender, age
          Text(
            modifier = Modifier.testTag(AGE_AND_BREED_TAG),
            text = "${animal.gender.displayName} – ${animal.age}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }
  }
}

private suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters {
  return show(
    BottomSheetOverlay(
      model = currentFilters,
      onDismiss = { currentFilters },
    ) { initialFilters, overlayNavigator ->
      Surface(Modifier.fillMaxWidth()) {
        UpdateFiltersSheet(
          initialFilters,
          Modifier.padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
          overlayNavigator::finish
        )
      }
    }
  )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUpdateFiltersSheet() {
  StarTheme {
    Surface {
      UpdateFiltersSheet(
        initialFilters = Filters(persistentSetOf(Gender.FEMALE)),
        modifier = Modifier.padding(16.dp),
      )
    }
  }
}

@VisibleForTesting
@Composable
internal fun UpdateFiltersSheet(
  initialFilters: Filters,
  modifier: Modifier = Modifier,
  onDismiss: (Filters) -> Unit = {}
) {
  Column(modifier.fillMaxWidth(), verticalArrangement = spacedBy(16.dp)) {
    val genderOptions = remember {
      SnapshotStateMap<Gender, Boolean>().apply {
        for (gender in Gender.values()) {
          put(gender, gender in initialFilters.genders)
        }
      }
    }
    FilterOptions("Gender", genderOptions)

    val sizeOptions = remember {
      SnapshotStateMap<Size, Boolean>().apply {
        for (size in Size.values()) {
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
              genders = genderOptions.filterValues { it }.keys.toImmutableSet(),
              sizes = sizeOptions.filterValues { it }.keys.toImmutableSet(),
            )
          onDismiss(newFilters)
        }
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
            leadingIcon = if (selected) leadingIcon else null
          )
        }
    }
  }
}

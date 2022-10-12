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
package com.slack.circuit.star.petlist

import android.content.res.Configuration
import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.LocalOverlayHost
import com.slack.circuit.Navigator
import com.slack.circuit.OverlayHost
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.star.R
import com.slack.circuit.star.data.Animal
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.overlay.BottomSheetOverlay
import com.slack.circuit.star.petdetail.PetDetailScreen
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import com.slack.circuit.star.repo.PetRepository
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

data class PetListAnimal(
  val id: Long,
  val name: String,
  val imageUrl: String?,
  val breed: String?,
  val gender: String,
  val size: String,
  val age: String,
)

enum class Gender {
  ALL,
  MALE,
  FEMALE
}

enum class Size {
  ALL,
  SMALL,
  MEDIUM,
  LARGE
}

@Parcelize
data class Filters(val gender: Gender = Gender.ALL, val size: Size = Size.ALL) : Parcelable

@Parcelize
object PetListScreen : Screen {
  sealed interface State : CircuitUiState {
    val isRefreshing: Boolean

    object Loading : State {
      override val isRefreshing: Boolean = false
    }

    data class NoAnimals(override val isRefreshing: Boolean) : State
    data class Success(
      val animals: List<PetListAnimal>,
      override val isRefreshing: Boolean,
      val filters: Filters = Filters(),
      val isUpdateFiltersModalShowing: Boolean = false,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class ClickAnimal(val petId: Long, val photoUrlMemoryCacheKey: String?) : Event
    object Refresh : Event
    object UpdateFilters : Event
    data class UpdatedFilters(val newFilters: Filters) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class PetListScreenPresenterFactory
@Inject
constructor(
  private val petListPresenterFactory: PetListPresenter.Factory,
) : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    circuitConfig: CircuitConfig
  ): Presenter<*>? {
    if (screen is PetListScreen) return petListPresenterFactory.create(navigator)
    return null
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
    val animalState by
      produceRetainedState<List<PetListAnimal>?>(null, isRefreshing) {
        val animals = petRepo.getAnimals(isRefreshing)
        isRefreshing = false
        value = animals.map { it.toPetListAnimal() }
      }

    var isUpdateFiltersModalShowing by rememberSaveable { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(Filters()) }

    val animals = animalState
    return when {
      animals == null -> PetListScreen.State.Loading
      animals.isEmpty() -> PetListScreen.State.NoAnimals(isRefreshing)
      else ->
        PetListScreen.State.Success(
          animals = animals.filter { shouldKeep(filters, it) },
          isRefreshing = isRefreshing,
          filters = filters,
          isUpdateFiltersModalShowing = isUpdateFiltersModalShowing,
        ) { event ->
          when (event) {
            is PetListScreen.Event.ClickAnimal -> {
              navigator.goTo(PetDetailScreen(event.petId, event.photoUrlMemoryCacheKey))
            }
            is PetListScreen.Event.UpdatedFilters -> {
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
    return filters.gender.shouldKeep(animal.gender) && filters.size.shouldKeep(animal.size)
  }

  private fun Gender.shouldKeep(gender: String): Boolean {
    if (this == Gender.ALL) return true
    return this.name.lowercase() == gender.lowercase()
  }

  private fun Size.shouldKeep(size: String): Boolean {
    if (this == Size.ALL) return true
    return this.name.lowercase() == size.lowercase()
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): PetListPresenter
  }
}

internal fun Animal.toPetListAnimal(): PetListAnimal {
  return PetListAnimal(
    id = id,
    // Names are sometimes all caps
    name = name.lowercase().capitalize(Locale.current),
    imageUrl = photos.firstOrNull()?.medium,
    breed = breeds.primary,
    gender = gender,
    size = size,
    age = age
  )
}

@ContributesMultibinding(AppScope::class)
class PetListUiFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, circuitConfig: CircuitConfig): ScreenUi? {
    if (screen is PetListScreen) {
      return ScreenUi(petListUi())
    }
    return null
  }
}

private fun petListUi() = ui<PetListScreen.State> { state -> PetList(state = state) }

internal object PetListTestConstants {
  const val PROGRESS_TAG = "progress"
  const val NO_ANIMALS_TAG = "no_animals"
  const val GRID_TAG = "grid"
  const val CARD_TAG = "card"
  const val IMAGE_TAG = "image"
}

@Composable
internal fun PetList(
  state: PetListScreen.State,
  modifier: Modifier = Modifier,
) {
  if (state is PetListScreen.State.Success && state.isUpdateFiltersModalShowing) {
    val eventSink = state.eventSink
    val overlayHost = LocalOverlayHost.current
    LaunchedEffect(state) {
      val result = overlayHost.updateFilters(state.filters)
      eventSink(PetListScreen.Event.UpdatedFilters(result))
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        },
        colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
        actions = {
          if (state is PetListScreen.State.Success) {
            val eventSink = state.eventSink
            IconButton(onClick = { eventSink(PetListScreen.Event.UpdateFilters) }) {
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
          CircularProgressIndicator(modifier = Modifier.testTag(PROGRESS_TAG))
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

@Composable
private fun PetListGrid(
  animals: List<PetListAnimal>,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier,
  eventSink: (PetListScreen.Event) -> Unit,
) {
  SwipeRefresh(
    state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
    onRefresh = { eventSink(PetListScreen.Event.Refresh) }
  ) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LazyVerticalGrid(
      columns = GridCells.Fixed(if (isLandscape) 3 else 2),
      modifier = modifier.testTag(GRID_TAG),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(16.dp),
    ) {
      items(
        count = animals.size,
        key = { i -> animals[i].id },
      ) { index ->
        val animal = animals[index]
        PetListGridItem(animal) {
          eventSink(PetListScreen.Event.ClickAnimal(animal.id, animal.imageUrl))
        }
      }
    }
  }
}

@Composable
private fun PetListGridItem(animal: PetListAnimal, onClick: () -> Unit = {}) {
  ElevatedCard(
    modifier = Modifier.fillMaxWidth().testTag(CARD_TAG),
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
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).testTag(IMAGE_TAG),
        model =
          ImageRequest.Builder(LocalContext.current)
            .data(animal.imageUrl)
            .memoryCacheKey(animal.imageUrl)
            .crossfade(true)
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
            text = "${animal.gender} – ${animal.age}",
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
      UpdateFiltersSheet(initialFilters, overlayNavigator::finish)
    }
  )
}

@Preview
@Composable
internal fun PreviewUpdateFiltersSheet() {
  Surface { UpdateFiltersSheet(initialFilters = Filters()) }
}

@Composable
private fun UpdateFiltersSheet(initialFilters: Filters, onDismiss: (Filters) -> Unit = {}) {
  var filters by remember { mutableStateOf(initialFilters) }
  Column(Modifier.fillMaxWidth()) {
    GenderFilterOption(filters.gender) { filters = filters.copy(gender = it) }
    SizeFilterOption(filters.size) { filters = filters.copy(size = it) }

    Row(Modifier.align(Alignment.End)) {
      Button(onClick = { onDismiss(initialFilters) }) { Text("Cancel") }
      Spacer(Modifier.width(16.dp))
      Button(onClick = { onDismiss(filters) }) { Text("Save") }
    }
  }
}

@Composable
private fun GenderFilterOption(
  selected: Gender,
  selectedGender: (Gender) -> Unit,
) {
  Box { Text(text = "Gender") }
  Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
    Gender.values().forEach { gender ->
      Column {
        Text(text = gender.name)
        RadioButton(selected = selected == gender, onClick = { selectedGender(gender) })
      }
    }
  }
}

@Composable
private fun SizeFilterOption(
  selected: Size,
  selectedSize: (Size) -> Unit,
) {
  Box { Text(text = "Size") }
  Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
    Size.values().forEach { size ->
      Column {
        Text(text = size.name)
        RadioButton(selected = selected == size, onClick = { selectedSize(size) })
      }
    }
  }
}

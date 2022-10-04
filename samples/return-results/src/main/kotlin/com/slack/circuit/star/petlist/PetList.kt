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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.star.R
import com.slack.circuit.star.data.Animal
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petlist.PetListTestConstants.CARD_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.GRID_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.IMAGE_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.NO_ANIMALS_TAG
import com.slack.circuit.star.petlist.PetListTestConstants.PROGRESS_TAG
import com.slack.circuit.star.petlistfilter.PetListFilterScreen
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
  val species: String,
  val breed: String?,
  val gender: String,
  val size: String,
  val age: String,
)

enum class Species {
  ALL,
  DOG,
  CAT
}

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
data class Filters(
  val species: Species = Species.ALL,
  val gender: Gender = Gender.ALL,
  val size: Size = Size.ALL
) : Parcelable

@Parcelize
object PetListScreen : Screen {
  interface WithEventSink {
    val eventSink: (Event) -> Unit
  }
  sealed interface State : CircuitUiState {
    object Loading : State
    data class NoAnimals(
      override val eventSink: (Event) -> Unit,
    ) : State, WithEventSink
    data class Success(
      val animals: List<PetListAnimal>,
      override val eventSink: (Event) -> Unit,
    ) : State, WithEventSink
  }

  sealed interface Event : CircuitUiEvent {
    object ClickFilters : Event
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
    val animalState by
      produceRetainedState<List<PetListAnimal>?>(null) {
        val animals = petRepo.getAnimals()
        value = animals.map { it.toPetListAnimal() }
      }
    var filters by remember { mutableStateOf(Filters()) }

    navigator.getResult<Filters> { result ->
      filters = result
    }

    return remember(filters, animalState) {
      val animals = animalState?.filter { it.shouldKeep(filters) }
      when {
        animals == null -> PetListScreen.State.Loading
        animals.isEmpty() -> PetListScreen.State.NoAnimals(buildEventSink(filters))
        else -> PetListScreen.State.Success(animals, buildEventSink(filters))
      }
    }
  }

  private fun buildEventSink(filters: Filters): (PetListScreen.Event) -> Unit {
    return { event ->
      when (event) {
        is PetListScreen.Event.ClickFilters -> navigator.goTo(PetListFilterScreen(filters))
      }
    }
  }

  private fun PetListAnimal.shouldKeep(filters: Filters): Boolean {
    return filters.species.shouldKeep(species)
      && filters.gender.shouldKeep(gender)
      && filters.size.shouldKeep(size)
  }

  private fun Species.shouldKeep(species: String): Boolean {
    if (this == Species.ALL) return true
    return this.name.lowercase() == species.lowercase()
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
    species = species,
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
  modifier: Modifier = Modifier,
  state: PetListScreen.State,
) {
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
          if (state is PetListScreen.WithEventSink) {
            IconButton(
              onClick = {
                state.eventSink(PetListScreen.Event.ClickFilters)
              }
            ) {
              Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter pet list",
                tint = MaterialTheme.colorScheme.onBackground
              )
            }
          }
        },
      )
    }
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
          modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
          animals = state.animals
        )
    }
  }
}

@Composable
private fun PetListGrid(
  modifier: Modifier = Modifier,
  animals: List<PetListAnimal>,
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
      PetListGridItem(animals[index])
    }
  }
}

@Composable
private fun PetListGridItem(animal: PetListAnimal) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(CARD_TAG),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      ),
  ) {
    Column {
      // Image
      AsyncImage(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .testTag(IMAGE_TAG),
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

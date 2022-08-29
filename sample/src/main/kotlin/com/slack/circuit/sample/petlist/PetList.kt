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
package com.slack.circuit.sample.petlist

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.slack.circuit.ContentContainer
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
import com.slack.circuit.collectEvents
import com.slack.circuit.sample.R
import com.slack.circuit.sample.data.Animal
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petdetail.PetDetailScreen
import com.slack.circuit.sample.repo.PetRepository
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class PetListAnimal(
  val id: Long,
  val name: String,
  val imageUrl: String?,
  val breed: String?,
  val gender: String,
  val age: String,
) : Parcelable

@Parcelize
object PetListScreen : Screen {
  sealed interface State : Parcelable {
    @Parcelize object Loading : State
    @Parcelize object NoAnimals : State
    @Parcelize data class Success(val animals: List<PetListAnimal>) : State
  }

  sealed interface Event {
    data class ClickAnimal(val petId: Long, val photoUrlMemoryCacheKey: String?) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class PetListScreenPresenterFactory
@Inject
constructor(private val petListPresenterFactory: PetListPresenter.Factory) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is PetListScreen) return petListPresenterFactory.create(navigator)
    return null
  }
}

class PetListPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val petRepo: PetRepository,
) : Presenter<PetListScreen.State, PetListScreen.Event> {
  @Composable
  override fun present(events: Flow<PetListScreen.Event>): PetListScreen.State {
    val state =
      produceState<PetListScreen.State>(PetListScreen.State.Loading) {
        val animals = petRepo.getAnimals()
        value =
          when {
            animals.isEmpty() -> PetListScreen.State.NoAnimals
            else -> PetListScreen.State.Success(animals.map { it.toPetListAnimal() })
          }
      }

    collectEvents(events) { event ->
      when (event) {
        is PetListScreen.Event.ClickAnimal -> {
          navigator.goTo(PetDetailScreen(event.petId, event.photoUrlMemoryCacheKey))
        }
      }
    }

    return state.value
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
    imageUrl = photos[0].medium.takeIf { it.startsWith("http") },
    breed = breeds.primary,
    gender = gender,
    age = age
  )
}

@ContributesMultibinding(AppScope::class)
class PetListScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen, container: ContentContainer): ScreenView? {
    if (screen is PetListScreen) {
      return ScreenView(container, petListUi())
    }
    return null
  }
}

private fun petListUi() =
  ui<PetListScreen.State, PetListScreen.Event> { state, events -> PetList(state, events) }

@Composable
internal fun PetList(state: PetListScreen.State, events: (PetListScreen.Event) -> Unit) {
  Scaffold(
    modifier = Modifier.systemBarsPadding().fillMaxWidth(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        },
        colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
      )
    },
  ) { paddingValues ->
    when (state) {
      PetListScreen.State.Loading ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.testTag("progress"))
        }
      PetListScreen.State.NoAnimals ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            modifier = Modifier.testTag("no animals"),
            text = stringResource(id = R.string.no_animals)
          )
        }
      is PetListScreen.State.Success ->
        PetListGrid(
          modifier = Modifier.padding(paddingValues).fillMaxSize(),
          animals = state.animals,
          events = events
        )
    }
  }
}

@Composable
private fun PetListGrid(
  modifier: Modifier = Modifier,
  animals: List<PetListAnimal>,
  events: (PetListScreen.Event) -> Unit
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = modifier.testTag("grid"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(16.dp),
  ) {
    items(
      count = animals.size,
      key = { i -> animals[i].id },
    ) { index ->
      val animal = animals[index]
      PetListGridItem(modifier, animal) {
        events(PetListScreen.Event.ClickAnimal(animal.id, animal.imageUrl))
      }
    }
  }
}

@Composable
private fun PetListGridItem(modifier: Modifier, animal: PetListAnimal, onClick: () -> Unit) {
  // Palette for extracted colors from the image
  var paletteState by remember { mutableStateOf<Palette?>(null) }
  val swatch = paletteState?.getSwatch()
  val defaultColors = CardDefaults.cardColors()
  val colors = swatch
    ?.let { s ->
      CardDefaults.cardColors(
        containerColor = Color(s.rgb),
      )
    }
    ?: defaultColors

  Card(
    modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() },
    colors = colors,
    shape = RoundedCornerShape(16.dp),
  ) {
    // Image
    AsyncImage(
      modifier = Modifier.fillMaxWidth().testTag("image"),
      model =
        ImageRequest.Builder(LocalContext.current)
          .data(animal.imageUrl)
          .fallback(R.drawable.dog)
          .memoryCacheKey(animal.imageUrl)
          .crossfade(true)
          // Default is hardware, which isn't usable in Palette
          .bitmapConfig(Bitmap.Config.ARGB_8888)
          .build(),
      contentDescription = animal.name,
      contentScale = ContentScale.FillWidth,
      onState = { state ->
        if (state is AsyncImagePainter.State.Success) {
          Palette.Builder(state.result.drawable.toBitmap()).generate { palette ->
            paletteState = palette
          }
        }
      }
    )
    Column(modifier.padding(8.dp), verticalArrangement = Arrangement.SpaceEvenly) {
      val textColor = swatch?.bodyTextColor?.let(::ComposeColor) ?: ComposeColor.Unspecified
      // Name
      Text(
        modifier = Modifier.testTag("name"),
        text = animal.name,
        style = MaterialTheme.typography.labelLarge,
        color = textColor
      )
      // Type
      animal.breed?.let {
        Text(
          modifier = Modifier.testTag("breed"),
          text = animal.breed,
          style = MaterialTheme.typography.bodyMedium,
          color = textColor
        )
      }
      CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        // Gender, age
        Text(
          modifier = Modifier.testTag("gender & age"),
          text = "${animal.gender} – ${animal.age}",
          style = MaterialTheme.typography.bodySmall,
          color = textColor
        )
      }
    }
  }
}

private fun Palette.getSwatch(): Swatch {
  return vibrantSwatch
    ?: lightVibrantSwatch ?: darkVibrantSwatch ?: lightMutedSwatch ?: mutedSwatch ?: darkMutedSwatch
      ?: error("No usable swatch found")
}

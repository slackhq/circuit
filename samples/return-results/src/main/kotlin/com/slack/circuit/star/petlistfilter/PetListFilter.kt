package com.slack.circuit.star.petlistfilter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.petlist.Filters
import com.slack.circuit.star.petlist.Gender
import com.slack.circuit.star.petlist.Size
import com.slack.circuit.star.petlist.Species
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class PetListFilterScreen(val filters: Filters = Filters()) : Screen {
  data class State(
    val filters: Filters,
    val showBottomSheet: Boolean,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class FilterBySpecies(val species: Species) : Event
    data class FilterByGender(val gender: Gender) : Event
    data class FilterBySize(val size: Size) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class PetListFilterScreenFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen, circuitConfig: CircuitConfig): ScreenUi? {
    if (screen is PetListFilterScreen) return ScreenUi(petListFilterUi())
    return null
  }
}

@ContributesMultibinding(AppScope::class)
class PetListFilterPresenterFactory
@Inject
constructor(private val petListFilterPresenterFactory: PetListFilterPresenter.Factory) :
  Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    circuitConfig: CircuitConfig
  ): Presenter<*>? {
    if (screen is PetListFilterScreen) return petListFilterPresenterFactory.create(navigator, screen)
    return null
  }
}

class PetListFilterPresenter @AssistedInject constructor(
  @Assisted private val navigator: Navigator,
  @Assisted private val screen: PetListFilterScreen
) : Presenter<PetListFilterScreen.State> {
  @Composable
  override fun present(): PetListFilterScreen.State {
    var filters by remember { mutableStateOf(screen.filters) }
    val showBottomSheet by remember { mutableStateOf(false) }

    return PetListFilterScreen.State(filters, showBottomSheet) { event ->
      filters = when (event) {
        is PetListFilterScreen.Event.FilterBySpecies -> filters.copy(species = event.species)
        is PetListFilterScreen.Event.FilterByGender -> filters.copy(gender = event.gender)
        is PetListFilterScreen.Event.FilterBySize -> filters.copy(size = event.size)
      }

      navigator.callbackResult(filters)
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator, screen: PetListFilterScreen): PetListFilterPresenter
  }
}

private fun petListFilterUi() = ui<PetListFilterScreen.State> { state -> PetListFilter(state) }

@Composable
internal fun PetListFilter(state: PetListFilterScreen.State) {
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text("Filter Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        },
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier.padding(paddingValues)
    ) {
      SpeciesFilterOption(state, state.eventSink)
      GenderFilterOption(state, state.eventSink)
      SizeFilterOption(state, state.eventSink)
    }
  }
}

@Composable
private fun SpeciesFilterOption(
  state: PetListFilterScreen.State,
  eventSink: (PetListFilterScreen.Event) -> Unit,
) {
  Box { Text(text = "Species") }
  Row(modifier = Modifier.selectableGroup()) {
    Species.values().forEach { species ->
      Column {
        Text(text = species.name)
        RadioButton(
          selected = state.filters.species == species,
          onClick = {
            eventSink(PetListFilterScreen.Event.FilterBySpecies(species))
          }
        )
      }
    }
  }
}

@Composable
private fun GenderFilterOption(
  state: PetListFilterScreen.State,
  eventSink: (PetListFilterScreen.Event) -> Unit,
) {
  Box { Text(text = "Gender") }
  Row(modifier = Modifier.selectableGroup()) {
    Gender.values().forEach { gender ->
      Column {
        Text(text = gender.name)
        RadioButton(
          selected = state.filters.gender == gender,
          onClick = {
            eventSink(PetListFilterScreen.Event.FilterByGender(gender))
          }
        )
      }
    }
  }
}

@Composable
private fun SizeFilterOption(
  state: PetListFilterScreen.State,
  eventSink: (PetListFilterScreen.Event) -> Unit,
) {
  Box { Text(text = "Size") }
  Row(modifier = Modifier.selectableGroup()) {
    Size.values().forEach { size ->
      Column {
        Text(text = size.name)
        RadioButton(
          selected = state.filters.size == size,
          onClick = { eventSink(PetListFilterScreen.Event.FilterBySize(size)) }
        )
      }
    }
  }
}

package com.slack.circuit.sample.petlist

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.slack.circuit.CircuitContent
import com.slack.circuit.EventCollector
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.UiFactory
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petlist.Gender.ALL as ALL_GENDERS
import com.slack.circuit.sample.petlist.Gender.FEMALE
import com.slack.circuit.sample.petlist.Gender.MALE
import com.slack.circuit.sample.petlist.Size.ALL as ALL_SIZES
import com.slack.circuit.sample.petlist.Size.LARGE
import com.slack.circuit.sample.petlist.Size.MEDIUM
import com.slack.circuit.sample.petlist.Size.SMALL
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Parcelize
object PetListFilterScreen : Screen {
  @Parcelize
  data class State(val gender: Gender, val size: Size, val showBottomSheet: Boolean) : Parcelable

  sealed interface Event {
    object ToggleAnimalFilter : Event
    data class FilterByGender(val gender: Gender) : Event
    data class FilterBySize(val size: Size) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class PetListFilterScreenFactory @Inject constructor() : UiFactory {
  override fun create(screen: Screen): ScreenUi? {
    if (screen is PetListFilterScreen) return ScreenUi(petListFilterUi())
    return null
  }
}

@ContributesMultibinding(AppScope::class)
class PetListFilterPresenterFactory
@Inject
constructor(private val petListFilterPresenterFactory: PetListFilterPresenter.Factory) :
  PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is PetListFilterScreen) return petListFilterPresenterFactory.create()
    return null
  }
}

class PetListFilterPresenter @AssistedInject constructor() :
  Presenter<PetListFilterScreen.State, PetListFilterScreen.Event> {
  @Composable
  override fun present(events: Flow<PetListFilterScreen.Event>): PetListFilterScreen.State {
    var state by remember {
      mutableStateOf(PetListFilterScreen.State(gender = Gender.ALL, size = Size.ALL, false))
    }

    EventCollector(events) { event ->
      state =
        when (event) {
          PetListFilterScreen.Event.ToggleAnimalFilter -> {
            state.copy(showBottomSheet = !state.showBottomSheet)
          }
          is PetListFilterScreen.Event.FilterByGender -> {
            state.copy(gender = event.gender)
          }
          is PetListFilterScreen.Event.FilterBySize -> {
            state.copy(size = event.size)
          }
        }
    }

    return state
  }

  @AssistedFactory
  interface Factory {
    fun create(): PetListFilterPresenter
  }
}

private fun petListFilterUi() =
  ui<PetListFilterScreen.State, PetListFilterScreen.Event> { state, events ->
    PetListFilter(state, events)
  }

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun PetListFilter(
  state: PetListFilterScreen.State,
  events: (PetListFilterScreen.Event) -> Unit
) {
  val modalState =
    rememberModalBottomSheetState(
      initialValue =
        if (state.showBottomSheet) ModalBottomSheetValue.Expanded else ModalBottomSheetValue.Hidden
    )

  // Monitor bottom sheet state and emit event whenever the user dismisses the modal
  LaunchedEffect(modalState) {
    snapshotFlow { modalState.isVisible }
      .collect { isVisible ->
        // Toggle if state says the modal should be visible but the snapshot says it isn't.
        if (state.showBottomSheet && !isVisible)
          events(PetListFilterScreen.Event.ToggleAnimalFilter)
      }
  }

  ModalBottomSheetLayout(
    sheetState = modalState,
    sheetContent = {
      Column {
        GenderFilterOption(state, events)
        SizeFilterOption(state, events)
      }
    }
  ) {
    Scaffold(
      modifier = Modifier.systemBarsPadding().fillMaxWidth(),
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            androidx.compose.material.Text(
              "Adoptables",
              fontSize = 22.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          },
          actions = {
            IconButton(onClick = { events(PetListFilterScreen.Event.ToggleAnimalFilter) }) {
              Icon(imageVector = Icons.Default.FilterList, contentDescription = "filter pet list", tint = Color.White)
            }
          },
          colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
      },
    ) { paddingValues ->
      Box(modifier = Modifier.padding(paddingValues)) {
        CircuitContent(PetListScreen(gender = state.gender, size = state.size))
      }
    }
  }
}

@Composable
private fun GenderFilterOption(
  state: PetListFilterScreen.State,
  events: (PetListFilterScreen.Event) -> Unit
) {
  Box { Text(text = "Gender") }
  Row(modifier = Modifier.selectableGroup()) {
    Column {
      Text(text = "All")
      RadioButton(
        selected = state.gender == ALL_GENDERS,
        onClick = { events(PetListFilterScreen.Event.FilterByGender(ALL_GENDERS)) }
      )
    }
    Column {
      Text(text = "Male")
      RadioButton(
        selected = state.gender == MALE,
        onClick = { events(PetListFilterScreen.Event.FilterByGender(MALE)) }
      )
    }
    Column {
      Text(text = "Female")
      RadioButton(
        selected = state.gender == FEMALE,
        onClick = { events(PetListFilterScreen.Event.FilterByGender(FEMALE)) }
      )
    }
  }
}

@Composable
private fun SizeFilterOption(
  state: PetListFilterScreen.State,
  events: (PetListFilterScreen.Event) -> Unit
) {
  Box { Text(text = "Size") }
  Row(modifier = Modifier.selectableGroup()) {
    Column {
      Text(text = "All")
      RadioButton(
        selected = state.size == ALL_SIZES,
        onClick = { events(PetListFilterScreen.Event.FilterBySize(ALL_SIZES)) }
      )
    }
    Column {
      Text(text = "Small")
      RadioButton(
        selected = state.size == SMALL,
        onClick = { events(PetListFilterScreen.Event.FilterBySize(SMALL)) }
      )
    }
    Column {
      Text(text = "Medium")
      RadioButton(
        selected = state.size == MEDIUM,
        onClick = { events(PetListFilterScreen.Event.FilterBySize(MEDIUM)) }
      )
    }
    Column {
      Text(text = "Large")
      RadioButton(
        selected = state.size == LARGE,
        onClick = { events(PetListFilterScreen.Event.FilterBySize(LARGE)) }
      )
    }
  }
}

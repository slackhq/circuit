package com.slack.circuit.sample.petlist

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.EventCollector
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.UiFactory
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

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
  ui<PetListFilterScreen.State, PetListFilterScreen.Event> { _, _ ->
    PetListFilter()
  }

@Composable
internal fun PetListFilter() {}

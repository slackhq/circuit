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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@Parcelize
object PetListFilterScreen : Screen {
  data class State(
    val filters: Filters,
    val showBottomSheet: Boolean,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    object ToggleAnimalFilter : Event
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
    if (screen is PetListFilterScreen) return petListFilterPresenterFactory.create()
    return null
  }
}

class PetListFilterPresenter @AssistedInject constructor() : Presenter<PetListFilterScreen.State> {
  @Composable
  override fun present(): PetListFilterScreen.State {
    var filters by remember { mutableStateOf(Filters()) }
    var showBottomSheet by remember { mutableStateOf(false) }

    return PetListFilterScreen.State(filters, showBottomSheet) { event ->
      when (event) {
        PetListFilterScreen.Event.ToggleAnimalFilter -> {
          showBottomSheet = !showBottomSheet
        }
        is PetListFilterScreen.Event.FilterByGender -> {
          filters = filters.copy(gender = event.gender)
        }
        is PetListFilterScreen.Event.FilterBySize -> {
          filters = filters.copy(size = event.size)
        }
      }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(): PetListFilterPresenter
  }
}

private fun petListFilterUi() = ui<PetListFilterScreen.State> { PetListFilter() }

@Composable internal fun PetListFilter() {}

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
package com.slack.circuit.sample.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Screen
import com.slack.circuit.sample.petlist.AboutScreen
import com.slack.circuit.sample.petlist.Filters
import com.slack.circuit.sample.petlist.PetListScreen
import com.slack.circuit.sample.petlist.ToggleFavoritePet
import kotlinx.parcelize.Parcelize

@Parcelize
data class HomeNavScreen(
  val filters: Filters,
//  val result: ToggleFavoritePet?
) : Screen {
  data class State(
    val index: Int,
    val currentScreen: Screen,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class ClickNavItem(val index: Int) : Event
    data class PetListResult(val favoritePet: ToggleFavoritePet) : Event
  }
}

@Composable
fun HomeNavPresenter(screen: HomeNavScreen): HomeNavScreen.State {
    var index by remember { mutableStateOf(0) }
    var petListResult by remember { mutableStateOf<ToggleFavoritePet?>(null) }

    val currentScreen = remember(screen, index, petListResult) {
      when (index) {
//        0 -> PetListScreen(filters = screen.filters, result = screen.result)
        0 -> PetListScreen(filters = screen.filters, result = petListResult)
        1 -> AboutScreen
        else -> error("Unknown nav index: $index")
      }
    }

    return HomeNavScreen.State(index = index, currentScreen = currentScreen) { event ->
      when (event) {
        is HomeNavScreen.Event.ClickNavItem -> index = event.index
        is HomeNavScreen.Event.PetListResult -> petListResult = event.favoritePet
      }
    }
}
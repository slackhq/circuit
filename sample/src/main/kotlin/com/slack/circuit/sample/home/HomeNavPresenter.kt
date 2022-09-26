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
import kotlinx.parcelize.Parcelize

private val HOME_NAV_ITEMS = listOf(BottomNavItem.Adoptables, BottomNavItem.About)

@Parcelize
object HomeNavScreen : Screen {
  data class State(
    val index: Int,
    val bottomNavItems: List<BottomNavItem>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class HomeNavEvent(val index: Int) : Event
  }
}

@Composable
fun homeNavPresenter(): HomeNavScreen.State {
  var index by remember { mutableStateOf(0) }
  return HomeNavScreen.State(index = index, bottomNavItems = HOME_NAV_ITEMS) { event ->
    when (event) {
      is HomeNavScreen.Event.HomeNavEvent -> index = event.index
    }
  }
}

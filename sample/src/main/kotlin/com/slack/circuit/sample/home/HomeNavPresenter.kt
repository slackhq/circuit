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
import com.slack.circuit.EventCollector
import com.slack.circuit.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

private val HOME_NAV_ITEMS = listOf(BottomNavItem.Adoptables, BottomNavItem.About)

@Parcelize
object HomeNavScreen : Screen {
  data class HomeNavState(val index: Int, val bottomNavItems: List<BottomNavItem>) : CircuitUiEvent

  sealed interface Event : CircuitUiEvent {
    data class HomeNavEvent(val index: Int) : Event
  }
}

@Composable
fun homeNavPresenter(events: Flow<HomeNavScreen.Event.HomeNavEvent>): HomeNavScreen.HomeNavState {
  var state by remember {
    mutableStateOf(HomeNavScreen.HomeNavState(index = 0, bottomNavItems = HOME_NAV_ITEMS))
  }

  // LaunchedEffect/EventCollector makes it take two clicks, figure it out when i come back.
  EventCollector(events) { event -> state = state.copy(index = event.index) }

  return state
}

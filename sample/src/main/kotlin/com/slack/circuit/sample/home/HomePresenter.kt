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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.sample.di.AppScope
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Parcelize
object HomeScreen : Screen {
  @Immutable data class State(val index: Int = 0, val bottomNavItems: List<Screen>)

  sealed interface Event {
    @Immutable data class NavClickEvent(val index: Int) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class HomeScreenPresenterFactory
@Inject
constructor(private val homePresenterFactory: HomePresenter) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is HomeScreen) return homePresenterFactory
    return null
  }
}

class HomePresenter @Inject constructor() : Presenter<HomeScreen.State, HomeScreen.Event> {
  private val homeScreenNavItems = listOf(BottomNavItem.Dogs.screen, BottomNavItem.Cats.screen)

  @Composable
  override fun present(events: Flow<HomeScreen.Event>): HomeScreen.State {
    var state by remember { mutableStateOf(HomeScreen.State(bottomNavItems = homeScreenNavItems)) }

    // LaunchedEffect makes it take two clicks, figure it out when i come back.
    LaunchedEffect(events) {
      events.collect { event ->
        when (event) {
          is HomeScreen.Event.NavClickEvent -> state = state.copy(index = event.index)
        }
      }
    }
    return state
  }
}

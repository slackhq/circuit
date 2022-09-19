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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.PresenterFactory
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.UiFactory
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petlist.About
import com.slack.circuit.sample.petlist.PetList
import com.slack.circuit.sample.petlist.PetListPresenter
import com.slack.circuit.sample.petlist.PetListScreen
import com.slack.circuit.ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize

@Parcelize
object HomeScreen : Screen {
  data class State(
    val homeNavState: HomeNavScreen.HomeNavState,
    val petListState: PetListScreen.State
  )

  sealed interface Event {
    class HomeEvent(val event: HomeNavScreen.Event.HomeNavEvent) : Event
    class PetListEvent(val event: PetListScreen.Event) : Event
  }
}

@ContributesMultibinding(AppScope::class)
class HomePresenterFactory
@Inject
constructor(private val homePresenterFactory: HomePresenter.Factory) : PresenterFactory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is HomeScreen) return homePresenterFactory.create(navigator)
    return null
  }
}

class HomePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  petListPresenterFactory: PetListPresenter.Factory
) : Presenter<HomeScreen.State, HomeScreen.Event> {
  private val petListPresenter = petListPresenterFactory.create(navigator)

  @SuppressLint("FlowOperatorInvokedInComposition")
  @Composable
  override fun present(events: Flow<HomeScreen.Event>): HomeScreen.State {
    val homeNavState =
      homeNavPresenter(events.filterIsInstance<HomeScreen.Event.HomeEvent>().map { it.event })
    val petListState =
      petListPresenter.present(
        events.filterIsInstance<HomeScreen.Event.PetListEvent>().map { it.event }
      )

    return HomeScreen.State(homeNavState, petListState)
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): HomePresenter
  }
}

@ContributesMultibinding(AppScope::class)
class HomeScreenFactory @Inject constructor() : UiFactory {
  override fun create(screen: Screen): ScreenUi? {
    if (screen is HomeScreen) {
      return ScreenUi(homeUi())
    }
    return null
  }
}

private fun homeUi() =
  ui<HomeScreen.State, HomeScreen.Event> { state, events -> HomeContent(state, events) }

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun HomeContent(state: HomeScreen.State, eventSink: (HomeScreen.Event) -> Unit) {
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
    bottomBar = {
      BottomNavigationBar(selectedIndex = state.homeNavState.index) { index ->
        eventSink(HomeScreen.Event.HomeEvent(HomeNavScreen.Event.HomeNavEvent(index)))
      }
    },
    content = {
      if (state.homeNavState.index == DOGS_SCREEN_INDEX) {
        PetList(state.petListState) { event -> eventSink(HomeScreen.Event.PetListEvent(event)) }
      } else if (state.homeNavState.index == ABOUT_SCREEN_INDEX) {
        About()
      }
    }
  )
}

@Composable
private fun BottomNavigationBar(selectedIndex: Int, onSelectedIndex: (Int) -> Unit) {
  // These are the buttons on the NavBar, they dictate where we navigate too.
  val items = listOf(BottomNavItem.Dogs, BottomNavItem.About)
  BottomNavigation(
    backgroundColor = MaterialTheme.colorScheme.onPrimaryContainer,
    contentColor = Color.White
  ) {
    items.forEachIndexed { index, item ->
      BottomNavigationItem(
        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
        label = { Text(text = item.title) },
        selectedContentColor = MaterialTheme.colorScheme.secondary,
        unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.4f),
        alwaysShowLabel = true,
        selected = selectedIndex == index,
        onClick = { onSelectedIndex(index) }
      )
    }
  }
}

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
import com.slack.circuit.ScreenView
import com.slack.circuit.ScreenViewFactory
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object HomeScreen : Screen {
  data class CompositeState(
    val homeNavState: HomeNavScreen.HomeNavState,
    val petListState: PetListScreen.State
  )

  sealed interface CompositeEvent {
    class CompositeHomeEvent(val event: HomeNavScreen.Event.HomeNavEvent) : CompositeEvent
    class CompositePetListEvent(val event: PetListScreen.Event) : CompositeEvent
  }
}

@ContributesMultibinding(AppScope::class)
class CompositePresenterFactory
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
) : Presenter<HomeScreen.CompositeState, HomeScreen.CompositeEvent> {
  private val petListPresenter = petListPresenterFactory.create(navigator)

  @SuppressLint("FlowOperatorInvokedInComposition")
  @Composable
  override fun present(
    events: Flow<HomeScreen.CompositeEvent>
  ): HomeScreen.CompositeState {
    val homeState =
      homeNavPresenter(
        events.filterIsInstance<HomeScreen.CompositeEvent.CompositeHomeEvent>().map {
          it.event
        }
      )
    val petListState =
      petListPresenter.present(
        events.filterIsInstance<HomeScreen.CompositeEvent.CompositePetListEvent>().map {
          it.event
        }
      )

    return HomeScreen.CompositeState(homeState, petListState)
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): HomePresenter
  }
}

@ContributesMultibinding(AppScope::class)
class CompositeScreenFactory @Inject constructor() : ScreenViewFactory {
  override fun createView(screen: Screen): ScreenView? {
    if (screen is HomeScreen) {
      return ScreenView(homeUi())
    }
    return null
  }
}

private fun homeUi() =
  ui<HomeScreen.CompositeState, HomeScreen.CompositeEvent> { state, events ->
    HomeContent(state, events)
  }

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun HomeContent(
  state: HomeScreen.CompositeState,
  eventSink: (HomeScreen.CompositeEvent) -> Unit
) {
  Scaffold(
    modifier = Modifier
      .systemBarsPadding()
      .fillMaxWidth(),
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
        eventSink(
          HomeScreen.CompositeEvent.CompositeHomeEvent(HomeNavScreen.Event.HomeNavEvent(index))
        )
      }
    },
    content = {
      if (state.homeNavState.index == DOGS_SCREEN_INDEX) {
        PetList(state.petListState) { event ->
          eventSink(HomeScreen.CompositeEvent.CompositePetListEvent(event))
        }
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

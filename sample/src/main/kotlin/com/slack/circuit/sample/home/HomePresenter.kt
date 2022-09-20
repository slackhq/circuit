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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.slack.circuit.CircuitContent
import com.slack.circuit.EventCollector
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.sample.di.AppScope
import com.slack.circuit.sample.petlist.About
import com.slack.circuit.sample.petlist.Gender
import com.slack.circuit.sample.petlist.PetListFilterPresenter
import com.slack.circuit.sample.petlist.PetListFilterScreen
import com.slack.circuit.sample.petlist.PetListScreen
import com.slack.circuit.sample.petlist.Size
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
  data class State(
    val homeNavState: HomeNavScreen.HomeNavState,
    val petListFilterState: PetListFilterScreen.State
  )

  sealed interface Event {
    class HomeEvent(val event: HomeNavScreen.Event.HomeNavEvent) : Event
    class PetListFilterEvent(val event: PetListFilterScreen.Event) : Event
    sealed interface ChildNavigation : Event {
      object Pop : ChildNavigation
      data class GoTo(val screen: Screen) : ChildNavigation
    }
  }
}

@ContributesMultibinding(AppScope::class)
class HomeScreenPresenterFactory
@Inject
constructor(private val homePresenterFactory: HomePresenter.Factory) : Presenter.Factory {
  override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    if (screen is HomeScreen) return homePresenterFactory.create(navigator)
    return null
  }
}

class HomePresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  petListFilterPresenterFactory: PetListFilterPresenter.Factory
) : Presenter<HomeScreen.State, HomeScreen.Event> {
  private val petListFilterPresenter = petListFilterPresenterFactory.create()

  @Composable
  override fun present(events: Flow<HomeScreen.Event>): HomeScreen.State {
    val rememberHomeNavState = remember { events.filterIsInstance<HomeScreen.Event.HomeEvent>().map { it.event } }
    val homeNavState = homeNavPresenter(rememberHomeNavState)

    val rememberPetListFilterState = remember { events.filterIsInstance<HomeScreen.Event.PetListFilterEvent>().map { it.event } }
    val petListFilterState = petListFilterPresenter.present(rememberPetListFilterState)

    val rememberChildNavigationEvent = remember { events.filterIsInstance<HomeScreen.Event.ChildNavigation>() }
    EventCollector(rememberChildNavigationEvent) { event ->
      when (event) {
        is HomeScreen.Event.ChildNavigation.GoTo -> navigator.goTo(event.screen)
        HomeScreen.Event.ChildNavigation.Pop -> navigator.pop()
      }
    }

    return HomeScreen.State(homeNavState, petListFilterState)
  }

  @AssistedFactory
  interface Factory {
    fun create(navigator: Navigator): HomePresenter
  }
}

@ContributesMultibinding(AppScope::class)
class HomeUiFactory @Inject constructor() : Ui.Factory {
  override fun create(screen: Screen): ScreenUi? {
    if (screen is HomeScreen) {
      return ScreenUi(homeUi())
    }
    return null
  }
}

private fun homeUi() =
  ui<HomeScreen.State, HomeScreen.Event> { state, events -> HomeContent(state, events) }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeContent(state: HomeScreen.State, eventSink: (HomeScreen.Event) -> Unit) {
  val childNavigator = rememberChildNavigator(eventSink)

  val modalState =
    rememberModalBottomSheetState(
      initialValue =
      if (state.petListFilterState.showBottomSheet) ModalBottomSheetValue.Expanded else ModalBottomSheetValue.Hidden
    )

  // Monitor bottom sheet state and emit event whenever the user dismisses the modal
  LaunchedEffect(modalState) {
    snapshotFlow { modalState.isVisible }
      .collect { isVisible ->
        // Toggle if state says the modal should be visible but the snapshot says it isn't.
        if (state.petListFilterState.showBottomSheet && !isVisible)
          eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.ToggleAnimalFilter))
      }
  }

  ModalBottomSheetLayout(
    sheetState = modalState,
    sheetContent = {
      Column {
        GenderFilterOption(state.petListFilterState, eventSink)
        SizeFilterOption(state.petListFilterState, eventSink)
      }
    }
  ) {
    Scaffold(
      modifier = Modifier
        .navigationBarsPadding()
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
          ),
          actions = {
            IconButton(onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.ToggleAnimalFilter)) }) {
              androidx.compose.material.Icon(imageVector = Icons.Default.FilterList, contentDescription = "filter pet list", tint = Color.White)
            }
          },
        )
      },
      bottomBar = {
        BottomNavigationBar(selectedIndex = state.homeNavState.index) { index ->
          eventSink(HomeScreen.Event.HomeEvent(HomeNavScreen.Event.HomeNavEvent(index)))
        }
      }
    ) { paddingValues ->
      if (state.homeNavState.index == DOGS_SCREEN_INDEX) {
        Box(modifier = Modifier.padding(paddingValues)) {
          CircuitContent(
            screen = PetListScreen(
              gender = state.petListFilterState.gender,
              size = state.petListFilterState.size
            ),
            navigator = childNavigator
          )
        }
      } else if (state.homeNavState.index == ABOUT_SCREEN_INDEX) {
        About()
      }
    }
  }
}

@Composable
private fun rememberChildNavigator(eventSink: (HomeScreen.Event) -> Unit) = remember {
  object : Navigator {
    override fun goTo(screen: Screen) {
      eventSink(HomeScreen.Event.ChildNavigation.GoTo(screen))
    }

    override fun pop(): Screen? {
      eventSink(HomeScreen.Event.ChildNavigation.Pop)
      return null
    }
  }
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

@Composable
private fun GenderFilterOption(
  state: PetListFilterScreen.State,
  eventSink: (HomeScreen.Event) -> Unit
) {
  Box { Text(text = "Gender") }
  Row(modifier = Modifier.selectableGroup()) {
    Column {
      Text(text = "All")
      RadioButton(
        selected = state.gender == Gender.ALL,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterByGender(Gender.ALL))) }
      )
    }
    Column {
      Text(text = "Male")
      RadioButton(
        selected = state.gender == Gender.MALE,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterByGender(Gender.MALE))) }
      )
    }
    Column {
      Text(text = "Female")
      RadioButton(
        selected = state.gender == Gender.FEMALE,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterByGender(Gender.FEMALE))) }
      )
    }
  }
}

@Composable
private fun SizeFilterOption(
  state: PetListFilterScreen.State,
  eventSink: (HomeScreen.Event) -> Unit
) {
  Box { Text(text = "Size") }
  Row(modifier = Modifier.selectableGroup()) {
    Column {
      Text(text = "All")
      RadioButton(
        selected = state.size == Size.ALL,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.ALL))) }
      )
    }
    Column {
      Text(text = "Small")
      RadioButton(
        selected = state.size == Size.SMALL,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.SMALL))) }
      )
    }
    Column {
      Text(text = "Medium")
      RadioButton(
        selected = state.size == Size.MEDIUM,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.MEDIUM))) }
      )
    }
    Column {
      Text(text = "Large")
      RadioButton(
        selected = state.size == Size.LARGE,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.LARGE))) }
      )
    }
  }
}

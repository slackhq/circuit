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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.slack.circuit.sample.petlist.About
import com.slack.circuit.sample.petlist.Gender
import com.slack.circuit.sample.petlist.PetListFilterPresenter
import com.slack.circuit.sample.petlist.PetListFilterScreen
import com.slack.circuit.sample.petlist.PetListPresenter
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
    val petListState: PetListScreen.State,
    val petListFilterState: PetListFilterScreen.State
  )

  sealed interface Event {
    data class NavTo(val screen: Screen) : Event
    object NavPop : Event
    class HomeEvent(val event: HomeNavScreen.Event.HomeNavEvent) : Event
    class PetListEvent(val event: PetListScreen.Event) : Event
    class PetListFilterEvent(val event: PetListFilterScreen.Event) : Event
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
  petListPresenterFactory: PetListPresenter.Factory,
  petListFilterPresenterFactory: PetListFilterPresenter.Factory
) : Presenter<HomeScreen.State, HomeScreen.Event> {
  private val petListPresenter = petListPresenterFactory.create(navigator, PetListScreen(Gender.ALL, Size.ALL))
  private val petListFilterPresenter = petListFilterPresenterFactory.create()

  @SuppressLint("FlowOperatorInvokedInComposition")
  @Composable
  override fun present(events: Flow<HomeScreen.Event>): HomeScreen.State {
    val homeNavState =
      homeNavPresenter(events.filterIsInstance<HomeScreen.Event.HomeEvent>().map { it.event })
    val petListFilterState = petListFilterPresenter.present(
      events.filterIsInstance<HomeScreen.Event.PetListFilterEvent>().map { it.event }
    )
    val petListState =
      petListPresenter.present(
        events.filterIsInstance<HomeScreen.Event.PetListEvent>().map { it.event }
      )

    EventCollector(events = events, eventCollector = { event ->
      when (event) {
        is HomeScreen.Event.NavTo -> navigator.goTo(event.screen)
        HomeScreen.Event.NavPop -> navigator.pop()
        else -> {}
      }
    })

    return HomeScreen.State(homeNavState, petListState, petListFilterState)
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

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeContent(state: HomeScreen.State, eventSink: (HomeScreen.Event) -> Unit) {
  val modifier = Modifier
    .navigationBarsPadding()
    .systemBarsPadding()
    .fillMaxWidth()

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
        eventSink( HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.ToggleAnimalFilter))
      }
  }

  ModalBottomSheetLayout(
    modifier = modifier,
    sheetState = modalState,
    sheetContent = {
      Column {
        GenderFilterOption(state.petListFilterState, eventSink)
        SizeFilterOption(state.petListFilterState, eventSink)
      }
    }
  ) {
    Scaffold(
      modifier = modifier,
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
          },
          actions = {
            IconButton(onClick = { eventSink( HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.ToggleAnimalFilter)) } ) {
              Icon(imageVector = Icons.Default.FilterList, contentDescription = "filter pet list", tint = Color.White)
            }
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
      }
    ) {
      if (state.homeNavState.index == DOGS_SCREEN_INDEX) {
        // I think CircuitContent might need to expose modifier.
        val nav = object : Navigator {
          override fun goTo(screen: Screen) {
            eventSink(HomeScreen.Event.NavTo(screen))
          }

          override fun pop(): Screen? {
            eventSink(HomeScreen.Event.NavPop)
            return null
          }
        }
        CircuitContent(PetListScreen(gender = state.petListFilterState.gender, size = state.petListFilterState.size), navigator = nav)
      } else if (state.homeNavState.index == ABOUT_SCREEN_INDEX) {
        About()
      }
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
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterByGender(Gender.ALL)) ) }
      )
    }
    Column {
      Text(text = "Male")
      RadioButton(
        selected = state.gender == Gender.MALE,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterByGender(Gender.MALE)) ) }
      )
    }
    Column {
      Text(text = "Female")
      RadioButton(
        selected = state.gender == Gender.FEMALE,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterByGender(Gender.FEMALE)) ) }
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
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.ALL)) ) }
      )
    }
    Column {
      Text(text = "Small")
      RadioButton(
        selected = state.size == Size.SMALL,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.SMALL)) ) }
      )
    }
    Column {
      Text(text = "Medium")
      RadioButton(
        selected = state.size == Size.MEDIUM,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.MEDIUM)) ) }
      )
    }
    Column {
      Text(text = "Large")
      RadioButton(
        selected = state.size == Size.LARGE,
        onClick = { eventSink(HomeScreen.Event.PetListFilterEvent(PetListFilterScreen.Event.FilterBySize(Size.LARGE)) ) }
      )
    }
  }
}


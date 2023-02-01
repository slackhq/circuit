// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.CircuitContent
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.NavEvent
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.onNavEvent
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.HomeScreen.Event.ChildNav
import com.slack.circuit.star.home.HomeScreen.Event.HomeEvent
import com.slack.circuit.star.ui.StarTheme
import kotlinx.parcelize.Parcelize

@Parcelize
object HomeScreen : Screen {
  data class State(
    val homeNavState: HomeNavScreen.State,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    class HomeEvent(val event: HomeNavScreen.Event) : Event
    class ChildNav(val navEvent: NavEvent) : Event
  }
}

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomePresenter(navigator: Navigator): HomeScreen.State {
  val homeNavState = HomeNavPresenter()
  return HomeScreen.State(homeNavState) { event ->
    when (event) {
      is HomeEvent -> homeNavState.eventSink(event.event)
      is ChildNav -> navigator.onNavEvent(event.navEvent)
    }
  }
}

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomeContent(state: HomeScreen.State, modifier: Modifier = Modifier) {
  val systemUiController = rememberSystemUiController()
  systemUiController.setStatusBarColor(MaterialTheme.colorScheme.background)
  systemUiController.setNavigationBarColor(MaterialTheme.colorScheme.primaryContainer)

  val eventSink = state.eventSink
  Scaffold(
    modifier = modifier.navigationBarsPadding().systemBarsPadding().fillMaxWidth(),
    bottomBar = {
      StarTheme(useDarkTheme = true) {
        BottomNavigationBar(selectedIndex = state.homeNavState.index) { index ->
          eventSink(HomeEvent(HomeNavScreen.Event.ClickNavItem(index)))
        }
      }
    }
  ) { paddingValues ->
    val screen = state.homeNavState.bottomNavItems[state.homeNavState.index].screen
    CircuitContent(
      screen,
      modifier = Modifier.padding(paddingValues),
      onNavEvent = { event -> eventSink(ChildNav(event)) }
    )
  }
}

@Composable
private fun BottomNavigationBar(selectedIndex: Int, onSelectedIndex: (Int) -> Unit) {
  // These are the buttons on the NavBar, they dictate where we navigate too.
  val items = listOf(BottomNavItem.Adoptables, BottomNavItem.About)
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
  ) {
    items.forEachIndexed { index, item ->
      NavigationBarItem(
        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
        label = { Text(text = item.title) },
        alwaysShowLabel = true,
        selected = selectedIndex == index,
        onClick = { onSelectedIndex(index) }
      )
    }
  }
}

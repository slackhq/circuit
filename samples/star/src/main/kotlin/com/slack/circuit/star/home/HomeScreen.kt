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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.HomeScreen.Event.ChildNav
import com.slack.circuit.star.home.HomeScreen.Event.ClickNavItem
import com.slack.circuit.star.ui.StarTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize

@Parcelize
object HomeScreen : Screen {
  data class State(
    val navItems: ImmutableList<BottomNavItem> =
      persistentListOf(BottomNavItem.Adoptables, BottomNavItem.About),
    val selectedIndex: Int = 0,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    class ClickNavItem(val index: Int) : Event
    class ChildNav(val navEvent: NavEvent) : Event
  }
}

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomePresenter(navigator: Navigator): HomeScreen.State {
  var selectedIndex by remember { mutableStateOf(0) }
  return HomeScreen.State(selectedIndex = selectedIndex) { event ->
    when (event) {
      is ClickNavItem -> {
        selectedIndex = event.index
      }
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
        BottomNavigationBar(selectedIndex = state.selectedIndex) { index ->
          eventSink(ClickNavItem(index))
        }
      }
    }
  ) { paddingValues ->
    val screen = state.navItems[state.selectedIndex].screen
    CircuitContent(
      screen,
      modifier = Modifier.padding(paddingValues),
      onNavEvent = { event -> eventSink(ChildNav(event)) }
    )
  }
}

@Composable
private fun BottomNavigationBar(selectedIndex: Int, onSelectedIndex: (Int) -> Unit) {
  // These are the buttons on the NavBar, they dictate where we navigate too
  //
  // NOTE: we wouldn't normally use rememberRetained here, but we want to have it in the
  // sample for our baseline profile generation
  val items = rememberRetained { listOf(BottomNavItem.Adoptables, BottomNavItem.About) }
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

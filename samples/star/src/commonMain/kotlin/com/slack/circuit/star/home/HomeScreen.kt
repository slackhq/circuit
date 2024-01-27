// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.common.Platform
import com.slack.circuit.star.di.AppScope
import com.slack.circuit.star.home.HomeScreen.Event.ClickNavItem
import com.slack.circuit.star.parcel.CommonParcelize
import com.slack.circuit.star.ui.StarTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@CommonParcelize
data object HomeScreen : Screen {
  data class State(
    val navItems: ImmutableList<BottomNavItem> =
      persistentListOf(BottomNavItem.Adoptables, BottomNavItem.About),
    val selectedIndex: Int = 0,
    val navigator: Navigator,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    class ClickNavItem(val index: Int) : Event
  }
}

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomePresenter(navigator: Navigator): HomeScreen.State {
  var selectedIndex by remember { mutableIntStateOf(0) }
  return HomeScreen.State(selectedIndex = selectedIndex, navigator = navigator) { event ->
    when (event) {
      is ClickNavItem -> {
        selectedIndex = event.index
      }
    }
  }
}

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomeContent(state: HomeScreen.State, modifier: Modifier = Modifier) {
  var contentComposed by rememberRetained { mutableStateOf(false) }
  Scaffold(
    modifier = modifier.fillMaxWidth(),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    containerColor = Color.Transparent,
    bottomBar = {
      StarTheme(useDarkTheme = true) {
        BottomNavigationBar(selectedIndex = state.selectedIndex) { index ->
          state.eventSink(ClickNavItem(index))
        }
      }
    },
  ) { paddingValues ->
    contentComposed = true
    val screen = state.navItems[state.selectedIndex].screen
    CircuitContent(screen, modifier = Modifier.padding(paddingValues), navigator = state.navigator)
  }
  Platform.ReportDrawnWhen { contentComposed }
}

@Composable
private fun BottomNavigationBar(selectedIndex: Int, onSelectedIndex: (Int) -> Unit) {
  // These are the buttons on the NavBar, they dictate where we navigate too
  val items = remember { listOf(BottomNavItem.Adoptables, BottomNavItem.About) }
  NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
    items.forEachIndexed { index, item ->
      NavigationBarItem(
        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
        label = { Text(text = item.title) },
        alwaysShowLabel = true,
        selected = selectedIndex == index,
        onClick = { onSelectedIndex(index) },
      )
    }
  }
}

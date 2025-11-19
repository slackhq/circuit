// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.progress
import com.slack.circuit.star.common.Platform
import com.slack.circuit.star.home.HomeScreen.Event.ChildNav
import com.slack.circuit.star.home.HomeScreen.Event.ClickNavItem
import com.slack.circuit.star.ui.StarTheme
import dev.zacsweers.metro.AppScope
import kotlin.math.roundToInt

@Parcelize
data object HomeScreen : Screen {

  data class State(
    val navItems: List<BottomNavItem> = listOf(BottomNavItem.Adoptables, BottomNavItem.About),
    val selectedIndex: Int = 0,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    class ClickNavItem(val index: Int) : Event

    class ChildNav(val navEvent: NavEvent) : Event

    data object Back : Event
  }
}

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomePresenter(navigator: Navigator): HomeScreen.State {
  var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
  return HomeScreen.State(selectedIndex = selectedIndex) { event ->
    when (event) {
      is ClickNavItem -> selectedIndex = event.index
      is ChildNav -> navigator.onNavEvent(event.navEvent)
      HomeScreen.Event.Back -> navigator.pop()
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomeContent(state: HomeScreen.State, modifier: Modifier = Modifier) =
  SharedElementTransitionScope {
    var contentComposed by rememberRetained { mutableStateOf(false) }
    Scaffold(
      modifier = modifier.fillMaxWidth().displayCutoutPadding(),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = Color.Transparent,
      bottomBar = {
        val scope = requireAnimatedScope(Navigation)
        val isInOverlay =
          isTransitionActive && scope.transition.targetState == EnterExitState.Visible
        val fraction by
          remember(scope) {
            derivedStateOf {
              val progress = scope.progress().value / .8f
              EaseInOutCubic.transform(progress.coerceIn(0f, 1f))
            }
          }
        StarTheme(useDarkTheme = true) {
          Layout(
            modifier = Modifier,
            measurePolicy = { measurables, constraints ->
              val placeable = measurables.first().measure(constraints)
              if (isInOverlay) {
                // Slide in the bottom bar
                val height = (placeable.height * fraction).roundToInt()
                layout(placeable.width, height) { placeable.place(IntOffset.Zero) }
              } else {
                layout(placeable.width, placeable.height) { placeable.place(IntOffset.Zero) }
              }
            },
            content = {
              BottomNavigationBar(
                selectedIndex = state.selectedIndex,
                onSelectedIndex = { index -> state.eventSink(ClickNavItem(index)) },
                modifier =
                  Modifier.renderInSharedTransitionScopeOverlay(
                    renderInOverlay = { isInOverlay },
                    zIndexInOverlay = 1f,
                  ),
              )
            },
          )
        }
      },
    ) { paddingValues ->
      // Create a single backstack that starts with the initial tab
      val backStack = rememberSaveableBackStack(root = state.navItems[state.selectedIndex].screen)
      val navigator =
        rememberCircuitNavigator(
          navStack = backStack,
          onRootPop = { state.eventSink(HomeScreen.Event.Back) },
        )

      // When tab changes, use resetRoot to switch tabs while preserving state
      val currentTabIndex = state.selectedIndex

      LaunchedEffect(currentTabIndex) {
        val currentScreen = state.navItems[currentTabIndex].screen
        val topScreen = backStack.topRecord?.screen
        if (topScreen != currentScreen) {
          navigator.resetRoot(currentScreen, StateOptions.SaveAndRestore)
        }
      }

      NavigableCircuitContent(
        navigator = navigator,
        backStack = backStack,
        modifier = Modifier.padding(paddingValues),
      )
      contentComposed = true
    }
    Platform.ReportDrawnWhen { contentComposed }
  }

// These are the buttons on the NavBar, they dictate where we navigate too
val NAV_ITEMS = listOf(BottomNavItem.Adoptables, BottomNavItem.About)

@Composable
private fun BottomNavigationBar(
  selectedIndex: Int,
  onSelectedIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
    NAV_ITEMS.forEachIndexed { index, item ->
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

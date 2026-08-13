// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.platform.testTag
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
import com.slack.circuit.serialization.CircuitSerializable
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.progress
import com.slack.circuit.star.common.Platform
import com.slack.circuit.star.home.HomeScreen.Event.ChildNav
import com.slack.circuit.star.home.HomeScreen.Event.ClickNavItem
import com.slack.circuit.star.home.HomeTestConstants.BOTTOM_NAVIGATION_TAG
import com.slack.circuit.star.home.HomeTestConstants.NAVIGATION_RAIL_ITEM_TAG
import com.slack.circuit.star.home.HomeTestConstants.NAVIGATION_RAIL_TAG
import com.slack.circuit.star.ui.StarTheme
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import dev.zacsweers.metro.AppScope
import kotlin.math.roundToInt

@Parcelize
@CircuitSerializable(AppScope::class)
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
    val scope = requireAnimatedScope(Navigation)
    val isInOverlay = isTransitionActive && scope.transition.targetState == EnterExitState.Visible
    val navigationVisibilityFraction by
      remember(scope) {
        derivedStateOf {
          val progress = scope.progress().floatValue / .8f
          EaseInOutCubic.transform(progress.coerceIn(0f, 1f))
        }
      }

    // Create a single backstack that starts with the initial tab
    val backStack = rememberSaveableBackStack(root = state.navItems[state.selectedIndex].screen)
    val navigator =
      rememberCircuitNavigator(
        backStack = backStack,
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

    HomeNavigationLayout(
      selectedIndex = state.selectedIndex,
      onSelectedIndex = { index -> state.eventSink(ClickNavItem(index)) },
      navigationModifier =
        Modifier.renderInSharedTransitionScopeOverlay(
          renderInOverlay = { isInOverlay },
          zIndexInOverlay = 1f,
        ),
      isInOverlay = isInOverlay,
      navigationVisibilityFraction = navigationVisibilityFraction,
      modifier = modifier,
    ) { paddingValues ->
      NavigableCircuitContent(
        navigator = navigator,
        backStack = backStack,
        modifier = Modifier.padding(paddingValues),
        decoratorFactory = remember { GestureNavigationDecorationFactory() },
      )
      contentComposed = true
    }
    Platform.ReportDrawnWhen { contentComposed }
  }

// These are the buttons on the NavBar, they dictate where we navigate too
val NAV_ITEMS = listOf(BottomNavItem.Adoptables, BottomNavItem.About)

internal object HomeTestConstants {
  const val BOTTOM_NAVIGATION_TAG = "bottom_navigation"
  const val NAVIGATION_RAIL_ITEM_TAG = "navigation_rail_item"
  const val NAVIGATION_RAIL_TAG = "navigation_rail"
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun HomeNavigationLayout(
  selectedIndex: Int,
  onSelectedIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
  navigationModifier: Modifier = Modifier,
  isInOverlay: Boolean = false,
  navigationVisibilityFraction: Float = 1f,
  content: @Composable (PaddingValues) -> Unit,
) {
  val useNavigationRail =
    calculateWindowSizeClass().heightSizeClass == WindowHeightSizeClass.Compact
  val navigationContent: @Composable () -> Unit = {
    StarTheme(useDarkTheme = true) {
      NavigationVisibilityLayout(
        useNavigationRail = useNavigationRail,
        isInOverlay = isInOverlay,
        fraction = navigationVisibilityFraction,
        modifier = navigationModifier,
      ) {
        if (useNavigationRail) {
          HomeNavigationRail(selectedIndex, onSelectedIndex)
        } else {
          BottomNavigationBar(selectedIndex, onSelectedIndex)
        }
      }
    }
  }

  val layoutModifier = if (useNavigationRail) modifier.fillMaxSize() else modifier.fillMaxWidth()
  Row(modifier = layoutModifier) {
    if (useNavigationRail) {
      navigationContent()
    }
    Scaffold(
      modifier =
        if (useNavigationRail) {
          Modifier.weight(1f)
            .fillMaxHeight()
            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        } else {
          Modifier.fillMaxWidth()
        },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = Color.Transparent,
      bottomBar = { if (!useNavigationRail) navigationContent() },
      content = content,
    )
  }
}

@Composable
private fun NavigationVisibilityLayout(
  useNavigationRail: Boolean,
  isInOverlay: Boolean,
  fraction: Float,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Layout(
    modifier = modifier,
    measurePolicy = { measurables, constraints ->
      val placeable = measurables.first().measure(constraints)
      val height =
        if (isInOverlay && !useNavigationRail) {
          (placeable.height * fraction).roundToInt()
        } else {
          placeable.height
        }
      layout(placeable.width, height) { placeable.place(IntOffset.Zero) }
    },
    content = content,
  )
}

@Composable
private fun BottomNavigationBar(
  selectedIndex: Int,
  onSelectedIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    modifier = modifier.testTag(BOTTOM_NAVIGATION_TAG),
  ) {
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

@Composable
private fun HomeNavigationRail(
  selectedIndex: Int,
  onSelectedIndex: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    windowInsets = NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Vertical),
    modifier = modifier.fillMaxHeight().testTag(NAVIGATION_RAIL_TAG),
  ) {
    Spacer(Modifier.weight(1f))
    NAV_ITEMS.forEachIndexed { index, item ->
      NavigationRailItem(
        modifier = Modifier.testTag("$NAVIGATION_RAIL_ITEM_TAG-$index"),
        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
        label = { Text(text = item.title) },
        alwaysShowLabel = true,
        selected = selectedIndex == index,
        onClick = { onSelectedIndex(index) },
      )
    }
    Spacer(Modifier.weight(1f))
  }
}

// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.rememberSaveableNavStack
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.navigation.intercepting.InterceptedGoToResult
import com.slack.circuitx.navigation.intercepting.InterceptedResetRootResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.Skipped
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor.Companion.SuccessConsumed
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator

@Parcelize data class ContentScreen(val tabs: List<TabScreen>) : Screen

data class ContentState(
  val tabs: List<TabScreen>,
  val rootScreen: Screen,
  val eventSink: (ContentEvent) -> Unit,
) : CircuitUiState

sealed interface ContentEvent : CircuitUiEvent {
  data class OnNavEvent(val event: NavEvent) : ContentEvent
}

class ContentPresenter(private val screen: ContentScreen, private val navigator: Navigator) :
  Presenter<ContentState> {
  @Composable
  override fun present(): ContentState {
    return ContentState(screen.tabs, screen.tabs.first()) { event ->
      when (event) {
        is ContentEvent.OnNavEvent -> navigator.onNavEvent(event.event)
      }
    }
  }

  object Factory : Presenter.Factory {
    override fun create(
      screen: Screen,
      navigator: Navigator,
      context: CircuitContext,
    ): Presenter<*>? {
      return if (screen is ContentScreen) {
        ContentPresenter(screen, navigator)
      } else {
        null
      }
    }
  }
}

object ContentUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return if (screen is ContentScreen) {
      ui<ContentState> { state, modifier -> ContentUi(state, modifier) }
    } else {
      null
    }
  }
}

private class ContentInterceptor(private val eventSink: State<(ContentEvent) -> Unit>) :
  NavigationInterceptor {
  override fun resetRoot(
    newRoot: Screen,
    options: Navigator.StateOptions,
    navigationContext: NavigationContext,
  ): InterceptedResetRootResult {
    return when (newRoot) {
      is TabScreen,
      is DetailScreen -> {
        Skipped
      }
      else -> {
        eventSink.value(ContentEvent.OnNavEvent(NavEvent.ResetRoot(newRoot, options)))
        SuccessConsumed
      }
    }
  }

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedGoToResult {
    return when (screen) {
      is TabScreen,
      is DetailScreen -> {
        Skipped
      }
      else -> {
        eventSink.value(ContentEvent.OnNavEvent(NavEvent.GoTo(screen)))
        SuccessConsumed
      }
    }
  }
}

@OptIn(
  ExperimentalCircuitApi::class,
  ExperimentalSharedTransitionApi::class,
  ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun ContentUi(state: ContentState, modifier: Modifier = Modifier) = SharedElementTransitionLayout {
  val eventSink = rememberUpdatedState(state.eventSink)
  val navStack = rememberSaveableNavStack(state.rootScreen)
  val contentInterceptor = remember { ContentInterceptor(eventSink) }
  val contentNavigator =
    rememberCircuitNavigator(navStack, onRootPop = {}, enableBackHandler = true)

  val interceptingNavigator =
    rememberInterceptingNavigator(
      navStack = navStack,
      navigator = contentNavigator,
      interceptors = listOf(contentInterceptor),
    )
  Scaffold(
    modifier = modifier.testTag(ContentTags.TAG_SCAFFOLD).fillMaxSize(),
    bottomBar = {
      BottomTabRow(
        tabs = state.tabs,
        rootScreen = navStack.rootRecord?.screen,
        onNavEvent = { interceptingNavigator.onNavEvent(it) },
      )
    },
  ) { innerPadding ->
    NavigableCircuitContent(
      navigator = interceptingNavigator,
      navStack = navStack,
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
      //      decoration =
      //        remember(circuit.animatedScreenTransforms, circuit.animatedNavDecoratorFactory) {
      //          AdaptiveListDetailNavDecoration(
      //            screenTransforms = circuit.animatedScreenTransforms,
      //            normalDecoratorFactory = circuit.animatedNavDecoratorFactory,
      //            detailPaneDecoratorFactory = circuit.animatedNavDecoratorFactory,
      //            showInDetailPane = { it.screen is DetailScreen },
      //          )
      //        },
    )
  }
}

@Composable
private fun BottomTabRow(
  tabs: List<TabScreen>,
  rootScreen: Screen?,
  onNavEvent: (NavEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)),
  ) {
    tabs.forEach { tab ->
      val selected = tab == rootScreen
      Text(
        text = tab.label,
        color = if (selected) MaterialTheme.colorScheme.onSecondary else Color.Unspecified,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier =
          Modifier.testTag(ContentTags.TAG_TAB)
            .semantics { this.selected = selected }
            .weight(1f)
            .height(IntrinsicSize.Max)
            .clickable {
              onNavEvent(NavEvent.ResetRoot(tab, saveState = true, restoreState = true))
            }
            .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Unspecified)
            .padding(horizontal = 8.dp, vertical = 20.dp),
      )
    }
    Icon(
      Icons.Default.Info,
      contentDescription = "Info",
      modifier =
        Modifier.clickable { onNavEvent(NavEvent.GoTo(InfoScreen)) }
          .height(IntrinsicSize.Max)
          .minimumInteractiveComponentSize()
          .padding(horizontal = 8.dp, vertical = 20.dp),
    )
  }
}

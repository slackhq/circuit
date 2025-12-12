// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.DelicateCircuitFoundationApi
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import kotlin.reflect.KClass

@Parcelize
sealed interface TabScreen : Screen {
  val label: String

  @Parcelize data class Root(override val label: String = "Root") : TabScreen

  @Parcelize data class Screen1(override val label: String = "Screen 1") : TabScreen

  @Parcelize data class Screen2(override val label: String = "Screen 2") : TabScreen

  @Parcelize data class Screen3(override val label: String = "Screen 3") : TabScreen

  fun next(): TabScreen {
    return when (this) {
      is Root -> Screen1()
      is Screen1 -> Screen2()
      is Screen2 -> Screen3()
      is Screen3 -> Root()
    }
  }

  companion object {
    val root = Root()
    val screen1 = Screen1()
    val screen2 = Screen2()
    val screen3 = Screen3()
    val all = listOf(root, screen1, screen2, screen3)
  }
}

object TabScreenCircuit {

  data class State(
    val label: String,
    val navStack: NavStackList<Screen>?,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object Next : Event

    data object Forward : Event

    data object Backward : Event
  }
}

class TabPresenter(private val screen: TabScreen, private val navigator: Navigator) :
  Presenter<TabScreenCircuit.State> {
  @Composable
  override fun present(): TabScreenCircuit.State {
    val navStack = navigator.peekNavStack()
    return TabScreenCircuit.State(label = screen.label, navStack = navStack) { event ->
      when (event) {
        is TabScreenCircuit.Event.Backward -> navigator.backward()
        is TabScreenCircuit.Event.Forward -> navigator.forward()
        is TabScreenCircuit.Event.Next -> navigator.goTo(screen.next())
      }
    }
  }

  class Factory(private val tabClass: KClass<out TabScreen>) : Presenter.Factory {
    override fun create(
      screen: Screen,
      navigator: Navigator,
      context: CircuitContext,
    ): Presenter<*>? {
      return if (tabClass.isInstance(screen)) {
        TabPresenter(screen as TabScreen, navigator)
      } else {
        null
      }
    }
  }
}

@OptIn(DelicateCircuitFoundationApi::class)
@Composable
fun TabUI(state: TabScreenCircuit.State, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
      Row(modifier = Modifier.align(Alignment.CenterStart)) {
        IconButton(
          enabled = state.navStack?.backwardItems?.firstOrNull() != null,
          onClick = { state.eventSink(TabScreenCircuit.Event.Backward) },
        ) {
          Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
        }
        IconButton(
          enabled = state.navStack?.forwardItems?.firstOrNull() != null,
          onClick = { state.eventSink(TabScreenCircuit.Event.Forward) },
        ) {
          Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = "Forward")
        }
      }
      Text(
        text = state.label,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.testTag(ContentTags.TAG_LABEL).align(Alignment.Center),
      )
    }
    LazyColumn(
      modifier =
        Modifier.fillMaxSize().testTag(ContentTags.TAG_CONTENT).clickable {
          state.eventSink(TabScreenCircuit.Event.Next)
        }
    ) {
      state.navStack?.let { navStack ->
        itemsIndexed(navStack.toList()) { i, item ->
          val isActive = navStack.active === item
          Text(
            text = "$i: $item",
            fontWeight = if (isActive) FontWeight.Bold else null,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }
    }
  }
}

class TabUiFactory(private val tabClass: KClass<out TabScreen>) : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
    return if (tabClass.isInstance(screen)) {
      ui<TabScreenCircuit.State> { state, modifier -> TabUI(state, modifier) }
    } else {
      null
    }
  }
}

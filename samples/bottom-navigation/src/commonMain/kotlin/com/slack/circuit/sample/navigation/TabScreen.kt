// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.slack.circuit.foundation.DelicateCircuitFoundationApi
import com.slack.circuit.foundation.LocalBackStack
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import kotlin.reflect.KClass
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Parcelize
sealed interface TabScreen : Screen, PrimaryScreen {
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
    val all = persistentListOf(root, screen1, screen2, screen3)
  }
}

object TabScreenCircuit {

  data class State(val label: String, val hasDetails: Boolean, val eventSink: (Event) -> Unit) :
    CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data object Next : Event

    data object Details : Event
  }
}

class TabPresenter(private val screen: TabScreen, private val navigator: Navigator) :
  Presenter<TabScreenCircuit.State> {
  @Composable
  override fun present(): TabScreenCircuit.State {
    return TabScreenCircuit.State(label = screen.label, hasDetails = screen is TabScreen.Screen1) {
      event ->
      when (event) {
        TabScreenCircuit.Event.Next -> navigator.goTo(screen.next())
        TabScreenCircuit.Event.Details -> navigator.goTo(DetailScreen(screen))
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
  val backStack = LocalBackStack.current?.toImmutableList() ?: persistentListOf()
  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        text = state.label,
        style = MaterialTheme.typography.headlineMedium,
        modifier =
          Modifier.testTag(ContentTags.TAG_LABEL).weight(1f).padding(top = 24.dp, bottom = 8.dp),
      )
      if (state.hasDetails) {
        Button(
          colors = ButtonDefaults.outlinedButtonColors(),
          onClick = { state.eventSink(TabScreenCircuit.Event.Details) },
        ) {
          Text(text = "View details")
        }
      }
    }
    LazyColumn(
      modifier =
        Modifier.fillMaxSize().testTag(ContentTags.TAG_CONTENT).clickable {
          state.eventSink(TabScreenCircuit.Event.Next)
        }
    ) {
      itemsIndexed(backStack) { i, item ->
        Text(
          text = "$i: ${item.screen}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
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

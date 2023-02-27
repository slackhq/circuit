package com.slack.circuit.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.managed.ManagingScreen
import com.slack.circuit.wizard.siblings.ChildScreen01
import kotlinx.parcelize.Parcelize

@Parcelize
object MainScreen : Screen {
  data class State(val eventSink: (ClickEvent) -> Unit) : CircuitUiState
  data class ClickEvent(val id: Int) : CircuitUiEvent
}

@CircuitInject(MainScreen::class, AppScope::class)
@Composable
fun mainPresenter(navigator: Navigator): MainScreen.State {
  return MainScreen.State {
    navigator.goTo(getChildScreen(it.id))
  }
}

private fun getChildScreen(id: Int): Screen = when (id) {
  1 -> ManagingScreen
  2 -> ChildScreen01
  else -> error("Unknown child ID: $id")
}

@CircuitInject(MainScreen::class, AppScope::class)
@Composable
fun MainUi(state: MainScreen.State, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(
      text = "Parent manages nested screens",
      modifier = Modifier.clickable { state.eventSink(MainScreen.ClickEvent(1)) }
    )
    Text(
      text = "Sibling screens",
      modifier = Modifier.clickable { state.eventSink(MainScreen.ClickEvent(2)) }
    )
  }
}
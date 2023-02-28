package com.slack.circuit.wizard.siblings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.AppScope
import kotlinx.parcelize.Parcelize

@Parcelize
object ChildScreen01 : Screen {
  data class State(val count: Int, val eventSink: (Event) -> Unit) : CircuitUiState
  sealed interface Event : CircuitUiEvent {
    object NextScreen : Event
    object Button : Event
    object Back : Event
  }
}

@CircuitInject(ChildScreen01::class, AppScope::class)
@Composable
fun childScreen01Presenter(navigator: Navigator): ChildScreen01.State {
  var count by rememberSaveable { mutableStateOf(0) }
  return ChildScreen01.State(count) { event ->
    when (event) {
      is ChildScreen01.Event.NextScreen -> navigator.goTo(ChildScreen02)
      is ChildScreen01.Event.Button -> count++
      is ChildScreen01.Event.Back -> navigator.pop()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ChildScreen01::class, AppScope::class)
@Composable
fun ChildScreen01Ui(state: ChildScreen01.State, modifier: Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = { ChildTopBar("Wizard: Child 01") { state.eventSink(ChildScreen01.Event.Back) } }
  ) { padding ->
    Column {
      Text(
        text = "Child 01 content: ${state.count}",
        modifier = modifier
          .padding(padding)
          .clickable { state.eventSink(ChildScreen01.Event.NextScreen) }
      )
      Button(onClick = { state.eventSink(ChildScreen01.Event.Button) }) {
        Text("click me!")
      }
    }
  }
}

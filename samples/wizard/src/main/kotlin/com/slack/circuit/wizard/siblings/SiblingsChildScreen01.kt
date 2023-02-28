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
object SiblingsChildScreen01 : Screen {
  data class State(val count: Int, val eventSink: (Event) -> Unit) : CircuitUiState
  sealed interface Event : CircuitUiEvent {
    object NextScreen : Event
    object Button : Event
    object Back : Event
  }
}

@CircuitInject(SiblingsChildScreen01::class, AppScope::class)
@Composable
fun siblingsChildScreen01Presenter(navigator: Navigator): SiblingsChildScreen01.State {
  var count by rememberSaveable { mutableStateOf(0) }
  return SiblingsChildScreen01.State(count) { event ->
    when (event) {
      is SiblingsChildScreen01.Event.NextScreen -> navigator.goTo(SiblingsChildScreen02)
      is SiblingsChildScreen01.Event.Button -> count++
      is SiblingsChildScreen01.Event.Back -> navigator.pop()
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(SiblingsChildScreen01::class, AppScope::class)
@Composable
fun SiblingsChildScreen01Ui(state: SiblingsChildScreen01.State, modifier: Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = { SiblingsTopBar("Wizard: Child 01") { state.eventSink(SiblingsChildScreen01.Event.Back) } }
  ) { padding ->
    Column {
      Text(
        text = "Child 01 content: ${state.count}",
        modifier = modifier
          .padding(padding)
          .clickable { state.eventSink(SiblingsChildScreen01.Event.NextScreen) }
      )
      Button(onClick = { state.eventSink(SiblingsChildScreen01.Event.Button) }) {
        Text("click me!")
      }
    }
  }
}

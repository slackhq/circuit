package com.slack.circuit.wizard.siblings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.AppScope
import kotlinx.parcelize.Parcelize

@Parcelize
object SiblingsChildScreen02 : Screen {
  class State(val eventSink: (Event) -> Unit) : CircuitUiState
  sealed interface Event : CircuitUiEvent {
    object Back : Event
  }
}

@CircuitInject(SiblingsChildScreen02::class, AppScope::class)
@Composable
fun siblingsChildScreen02Presenter(navigator: Navigator) = SiblingsChildScreen02.State {
  navigator.pop()
}

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(SiblingsChildScreen02::class, AppScope::class)
@Composable
fun SiblingsChildScreen02Ui(state: SiblingsChildScreen02.State, modifier: Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = { SiblingsTopBar("Wizard: Child 02") { state.eventSink(SiblingsChildScreen02.Event.Back) } }
  ) { padding ->
    Text(
      text = "Child 02 content",
      modifier = modifier.padding(padding)
    )
  }
}

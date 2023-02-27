package com.slack.circuit.wizard.managed

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.wizard.AppScope
import kotlinx.parcelize.Parcelize

@Parcelize
object ManagedChildScreen01 : ManagedChildScreen {
  data class State(val eventSink: (ClickEvent) -> Unit) : CircuitUiState
  object ClickEvent : CircuitUiEvent
}

@CircuitInject(ManagedChildScreen01::class, AppScope::class)
@Composable
fun managedChildScreen01Presenter(navigator: Navigator) = ManagedChildScreen01.State {
  navigator.goTo(ManagedChildScreen02)
}

@CircuitInject(ManagedChildScreen01::class, AppScope::class)
@Composable
fun ManagedChildScreen01Ui(state: ManagedChildScreen01.State, modifier: Modifier) {
  Text(
    text = "Child 01 content",
    modifier = modifier.clickable { state.eventSink(ManagedChildScreen01.ClickEvent) }
  )
}

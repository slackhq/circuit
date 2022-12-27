package com.slack.circuit.sample.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.slack.circuit.CircuitContext
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenResultHandler
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.presenterOf
import com.slack.circuit.ui
import kotlinx.parcelize.Parcelize

@Parcelize object ChildScreen : Screen {
  class State(val eventSink: (Event) -> Unit) : CircuitUiState
  data class Event(val name: String) : CircuitUiEvent
}

@Composable
fun childPresenter(resultHandler: ScreenResultHandler, navigator: Navigator): ChildScreen.State {
  return ChildScreen.State { event ->
    resultHandler(ParentScreen.ChildResult(event.name))
    navigator.pop()
  }
}

@Composable
fun ChildUi(state: ChildScreen.State) {
  var name by remember { mutableStateOf("") }
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center
  ) {
    TextField(
      modifier = Modifier.fillMaxWidth(),
      value = name,
      onValueChange = { name = it },
      label = { Text(text = "Name") }
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Return name to: ")
      Button(onClick = { state.eventSink(ChildScreen.Event(name)) }) {
        Text(text = "Screen")
      }
    }
  }
}

@Preview
@Composable
private fun ChildUiPreview() {
  ChildUi(ChildScreen.State { })
}

internal object ChildPresenterFactory : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    resultHandler: ScreenResultHandler,
    context: CircuitContext
  ): Presenter<*>? = when (screen) {
    is ChildScreen -> presenterOf { childPresenter(resultHandler, navigator) }
    else -> null
  }
}

internal object ChildUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): ScreenUi? = when (screen) {
    is ChildScreen -> ScreenUi(ui<ChildScreen.State> { state -> ChildUi(state) })
    else -> null
  }
}

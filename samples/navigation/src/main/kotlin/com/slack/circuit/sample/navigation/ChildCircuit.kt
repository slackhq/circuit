package com.slack.circuit.sample.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
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
  sealed interface Event : CircuitUiEvent {
    val name: String
    data class ReturnName1(override val name: String) : Event
    data class ReturnName2(override val name: String) : Event
  }
}

@Composable
fun childPresenter(resultHandler: ScreenResultHandler, navigator: Navigator): ChildScreen.State {
  return ChildScreen.State { event ->
    val result = when (event) {
      is ChildScreen.Event.ReturnName1 -> ParentScreen.ChildResult(event.name)
      is ChildScreen.Event.ReturnName2 -> InterceptedResult(event.name)
    }
    resultHandler(result)
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
      Text("Return name to:")
      Spacer(modifier = Modifier.width(5.dp))
      Button(onClick = { state.eventSink(ChildScreen.Event.ReturnName1(name)) }) {
        Text(text = "Screen")
      }
      Spacer(modifier = Modifier.width(5.dp))
      Button(onClick = { state.eventSink(ChildScreen.Event.ReturnName2(name)) }) {
        Text(text = "Activity")
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

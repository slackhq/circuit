package com.slack.circuit.sample.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.slack.circuit.CircuitContext
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.ScreenResult
import com.slack.circuit.ScreenResultHandler
import com.slack.circuit.ScreenUi
import com.slack.circuit.Ui
import com.slack.circuit.presenterOf
import com.slack.circuit.ui
import kotlinx.parcelize.Parcelize

@Parcelize data class ParentScreen(val name: String = "") : Screen {
  data class State(val name: String, val eventSink: (GoToChild) -> Unit) : CircuitUiState

  object GoToChild : CircuitUiEvent

  @Parcelize data class ChildResult(val name: String) : ScreenResult
}

@Composable
fun parentPresenter(screen: ParentScreen, navigator: Navigator): ParentScreen.State {
  val name = screen.name.ifBlank { "" }
  return ParentScreen.State(name) {
    navigator.goTo(ChildScreen)
  }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun ParentUi(state: ParentScreen.State) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (state.name.isNotBlank()) {
      Text(
        text = "Hello, ${state.name}!",
        fontSize = TextUnit(50f, TextUnitType.Sp)
      )
    }
    Button(onClick = { state.eventSink(ParentScreen.GoToChild) }) {
      Text(text = "Get name")
    }
  }
}

@Preview
@Composable
fun ParentUiPreview() {
  ParentUi(ParentScreen.State("") {})
}

@Preview
@Composable
fun ParentUiWithNamePreview() {
  ParentUi(ParentScreen.State("Joe") {})
}

internal object ParentPresenterFactory : Presenter.Factory {
  override fun create(
    screen: Screen,
    navigator: Navigator,
    resultHandler: ScreenResultHandler,
    context: CircuitContext
  ): Presenter<*>? = when (screen) {
    is ParentScreen -> presenterOf { parentPresenter(screen, navigator) }
    else -> null
  }
}

internal object ParentUiFactory : Ui.Factory {
  override fun create(screen: Screen, context: CircuitContext): ScreenUi? = when (screen) {
    is ParentScreen -> ScreenUi(ui<ParentScreen.State> { state -> ParentUi(state) })
    else -> null
  }
}
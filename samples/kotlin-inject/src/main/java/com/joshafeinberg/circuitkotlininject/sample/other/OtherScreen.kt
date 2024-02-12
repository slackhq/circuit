package com.joshafeinberg.circuitkotlininject.sample.other

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.joshafeinberg.circuitkotlininject.sample.AppScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen

@CircuitInject(OtherScreen::class, AppScope::class)
@Composable
fun OtherScreen(modifier: Modifier = Modifier) {
  Text("Other Screen", modifier = modifier)
}

data object OtherScreen : Screen {
  data object OtherScreenState : CircuitUiState
}

@CircuitInject(OtherScreen::class, AppScope::class)
class OtherScreenPresenter : Presenter<OtherScreen.OtherScreenState> {
  @Composable
  override fun present(): OtherScreen.OtherScreenState {
    return OtherScreen.OtherScreenState
  }
}

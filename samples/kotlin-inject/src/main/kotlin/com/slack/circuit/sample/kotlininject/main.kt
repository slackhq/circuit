// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.kotlininject

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

typealias KotlinInjectApp = @Composable () -> Unit

fun main() = application {
  val app = remember { AppComponent::class.create().kotlinInjectApp }

  Window(onCloseRequest = ::exitApplication, title = "Sample") { app() }
}

@Inject
@Composable
fun KotlinInjectApp(circuit: Circuit) {
  CircuitCompositionLocals(circuit) { CircuitContent(MyScreen("Circuit")) }
}

@CircuitInject(MyScreen::class, AppScope::class)
@Composable
fun MyScreen(state: MyScreen.State, modifier: Modifier = Modifier) {
  Text(state.visibleString, modifier = modifier)
}

data class MyScreen(val inputText: String) : Screen {
  data class State(val visibleString: String) : CircuitUiState
}

@CircuitInject(MyScreen::class, AppScope::class)
@Inject
class MyScreenPresenter(
  private val injectedString: String,
  @Assisted private val screen: MyScreen,
) : Presenter<MyScreen.State> {
  @Composable
  override fun present(): MyScreen.State {
    return MyScreen.State(screen.inputText + " + " + injectedString)
  }
}

/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
public fun CircuitContent(
  screen: Screen,
  unavailableContent: (@Composable () -> Unit)? = null,
) {
  CircuitContent(screen, Navigator.NoOp, unavailableContent)
}

@Composable
internal fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  unavailableContent: (@Composable () -> Unit)? = null,
) {
  val circuit = LocalCircuitOwner.current

  @Suppress("UNCHECKED_CAST") val ui = circuit.ui(screen) as Ui<Any, Any>?

  @Suppress("UNCHECKED_CAST")
  val presenter = circuit.presenter(screen, navigator) as Presenter<Any, Any>?

  if (ui != null && presenter != null) {
    CircuitRender(presenter, ui)
  } else if (unavailableContent != null) {
    unavailableContent()
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState : Any, UiEvent : Any> CircuitRender(
  presenter: Presenter<UiState, UiEvent>,
  ui: Ui<UiState, UiEvent>,
) {
  val channel = remember(presenter, ui) { Channel<UiEvent>(BUFFERED) }
  val eventsFlow = remember(channel) { channel.receiveAsFlow() }
  val state = presenter.present(eventsFlow)
  ui.Render(state) { event -> channel.trySend(event) }
}

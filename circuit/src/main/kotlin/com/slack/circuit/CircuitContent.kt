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

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.NavigatorDefaults
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.providedValuesForBackStack
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.shareIn

@Composable
fun NavigableCircuitContent(
  navigator: Navigator,
  backstack: SaveableBackStack,
  modifier: Modifier = Modifier,
  circuit: Circuit = LocalCircuitOwner.current,
  enableBackHandler: Boolean = true,
  providedValues: Map<out BackStack.Record, ProvidedValues> = providedValuesForBackStack(backstack),
  decoration: NavDecoration = NavigatorDefaults.DefaultDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  BackHandler(enabled = enableBackHandler && !backstack.isAtRoot) { navigator.pop() }

  BasicNavigableCircuitContent(
    navigator = navigator,
    backstack = backstack,
    providedValues = providedValues,
    modifier = modifier,
    circuit = circuit,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

@Composable
fun BasicNavigableCircuitContent(
  navigator: Navigator,
  backstack: SaveableBackStack,
  providedValues: Map<out BackStack.Record, ProvidedValues>,
  modifier: Modifier = Modifier,
  circuit: Circuit = LocalCircuitOwner.current,
  decoration: NavDecoration = NavigatorDefaults.EmptyDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  val activeContentProviders = buildList {
    for (record in backstack) {
      val provider =
        key(record.key) {
          val routeName = record.route
          val screen = record.screen

          val currentRender: (@Composable (SaveableBackStack.Record) -> Unit) = {
            CircuitContent(screen, navigator, circuit) { unavailableRoute(routeName) }
          }

          val currentRouteContent by rememberUpdatedState(currentRender)
          val currentRecord by rememberUpdatedState(record)
          remember { movableContentOf { currentRouteContent(currentRecord) } }
        }
      add(record to provider)
    }
  }

  if (backstack.size > 0) {
    @Suppress("SpreadOperator")
    decoration.DecoratedContent(activeContentProviders.first(), backstack.size, modifier) {
      (record, provider) ->
      val values = providedValues[record]?.provideValues()
      val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }
      CompositionLocalProvider(*providedLocals) { provider() }
    }
  }
}

@Composable
fun CircuitContent(
  screen: Screen,
  circuit: Circuit = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuit.onUnavailableContent,
) {
  CircuitContent(screen, Navigator.NoOp, circuit, unavailableContent)
}

@Composable
private fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  circuit: Circuit,
  unavailableContent: (@Composable (screen: Screen) -> Unit)?,
) {
  @Suppress("UNCHECKED_CAST") val ui = circuit.ui(screen) as Ui<CircuitUiState, CircuitUiEvent>?

  @Suppress("UNCHECKED_CAST")
  val presenter = circuit.presenter(screen, navigator) as Presenter<CircuitUiState, CircuitUiEvent>?

  if (ui != null && presenter != null) {
    CircuitRender(presenter, ui)
  } else if (unavailableContent != null) {
    unavailableContent(screen)
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState : CircuitUiState, UiEvent : CircuitUiEvent> CircuitRender(
  presenter: Presenter<UiState, UiEvent>,
  ui: Ui<UiState, UiEvent>,
) {
  val channel = remember(presenter, ui) { Channel<UiEvent>(BUFFERED) }
  val scope = rememberCoroutineScope()
  val eventsFlow =
    remember(channel, scope) { channel.consumeAsFlow().shareIn(scope, SharingStarted.Lazily) }
  val state = presenter.present(eventsFlow)
  ui.Render(state) { event ->
    val result = channel.trySend(event)
    if (!result.isSuccess && !result.isClosed) {
      error("Event buffer overflow")
    }
  }
}

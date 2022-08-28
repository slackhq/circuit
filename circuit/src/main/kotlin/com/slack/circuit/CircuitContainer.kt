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

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.NavigatorDefaults
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.providedValuesForBackStack
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun NavigableCircuitContainer(
  circuit: Circuit,
  navigator: Navigator,
  modifier: Modifier = Modifier,
  enableBackHandler: Boolean = true,
  providedValues: Map<out BackStack.Record, ProvidedValues> =
    providedValuesForBackStack(navigator.backstack),
  decoration: NavDecoration = NavigatorDefaults.DefaultDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  BackHandler(enabled = enableBackHandler && !navigator.isAtRoot) { navigator.pop() }

  BasicNavigableCircuitContainer(
    circuit = circuit,
    navigator = navigator,
    providedValues = providedValues,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

@Composable
fun BasicNavigableCircuitContainer(
  circuit: Circuit,
  navigator: Navigator,
  providedValues: Map<out BackStack.Record, ProvidedValues>,
  modifier: Modifier = Modifier,
  decoration: NavDecoration = NavigatorDefaults.EmptyDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  val activeContentProviders = buildList {
    for (record in navigator.backstack) {
      val provider =
        key(record.key) {
          val routeName = record.route
          // TODO can we push this into the API somehow? Maybe make args part of BackStack.Record?
          val screen = (record as SaveableBackStack.Record).screen

          val currentRender: (@Composable (SaveableBackStack.Record) -> Unit) = {
            CircuitContainer(circuit, screen, navigator) { unavailableRoute(routeName) }
          }

          val currentRouteContent by rememberUpdatedState(currentRender)
          val currentRecord by rememberUpdatedState(record)
          remember { movableContentOf { currentRouteContent(currentRecord) } }
        }
      add(record to provider)
    }
  }

  if (navigator.backstack.size > 0) {
    @Suppress("SpreadOperator")
    decoration.DecoratedContent(
      activeContentProviders.first(),
      navigator.backstack.size,
      modifier
    ) { (record, provider) ->
      val values = providedValues[record]?.provideValues()
      val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }
      CompositionLocalProvider(*providedLocals) { provider.invoke() }
    }
  }
}

// TODO would be cool to expose circuit as a providedvalue
@Composable
fun CircuitContainer(
  circuit: Circuit,
  screen: Screen,
  unavailableContent: (@Composable () -> Unit)? = null,
) {
  CircuitContainer(circuit, screen, Navigator.NoOp, unavailableContent)
}

@Composable
private fun CircuitContainer(
  circuit: Circuit,
  screen: Screen,
  navigator: Navigator,
  unavailableContent: (@Composable () -> Unit)? = null,
) {
  @Suppress("UNCHECKED_CAST") val ui = circuit.ui(screen) as Ui<Parcelable, Any>?

  @Suppress("UNCHECKED_CAST")
  val presenter = circuit.presenter(screen, navigator) as Presenter<Parcelable, Any>?

  if (ui != null && presenter != null) {
    CircuitContainer(presenter, ui)
  } else if (unavailableContent != null) {
    unavailableContent()
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState, UiEvent : Any> CircuitContainer(
  presenter: Presenter<UiState, UiEvent>,
  ui: Ui<UiState, UiEvent>,
) where UiState : Parcelable, UiState : Any {
  val channel = remember(presenter, ui) { Channel<UiEvent>(BUFFERED) }
  val eventsFlow = remember(channel) { channel.receiveAsFlow() }
  val state = presenter.present(eventsFlow)
  ui.Render(state) { event -> channel.trySend(event) }
}

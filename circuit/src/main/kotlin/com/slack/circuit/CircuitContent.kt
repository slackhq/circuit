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

import android.util.Log
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
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.providedValuesForBackStack

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

          val currentContent: (@Composable (SaveableBackStack.Record) -> Unit) = {
            CircuitContent(screen, navigator, circuit) { unavailableRoute(routeName) }
          }

          val currentRouteContent by rememberUpdatedState(currentContent)
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
fun CircuitContent(
  screen: Screen,
  onNavEvent: (event: NavEvent) -> Unit,
  circuit: Circuit = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuit.onUnavailableContent,
) {
  val navigator =
    remember(onNavEvent) {
      object : Navigator {
        override fun goTo(screen: Screen) {
          onNavEvent(GoToNavEvent(screen))
        }

        override fun pop(result: Any?): Screen? {
          onNavEvent(PopNavEvent(result))
          return null
        }

        override fun <T : Any> maybeGetResult(): T? {
          Log.d("kier", "requesting result from anonymous navigator!")
          return null
        }
      }
    }
  CircuitContent(screen, navigator, circuit, unavailableContent)
}

@Composable
private fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  circuit: Circuit,
  unavailableContent: (@Composable (screen: Screen) -> Unit)?,
) {
  val screenUi = circuit.ui(screen)

  @Suppress("UNCHECKED_CAST")
  val presenter = circuit.presenter(screen, navigator) as Presenter<CircuitUiState>?

  if (screenUi != null && presenter != null) {
    @Suppress("UNCHECKED_CAST") (CircuitContent(presenter, screenUi.ui as Ui<CircuitUiState>))
  } else if (unavailableContent != null) {
    unavailableContent(screen)
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState : CircuitUiState> CircuitContent(
  presenter: Presenter<UiState>,
  ui: Ui<UiState>,
) {
  val state = presenter.present()
  ui.Content(state)
}

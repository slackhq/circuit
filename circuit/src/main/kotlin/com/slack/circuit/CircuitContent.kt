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
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.backstack.NavigatorDefaults
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.providedValuesForBackStack
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun NavigableCircuitContent(
  navigator: Navigator,
  backstack: SaveableBackStack,
  modifier: Modifier = Modifier,
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
            CircuitContent(screen, navigator) { unavailableRoute(routeName) }
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
      CompositionLocalProvider(*providedLocals) { provider.invoke() }
    }
  }
}

@Composable
fun CircuitContent(
  screen: Screen,
  unavailableContent: (@Composable () -> Unit)? = null,
) {
  CircuitContent(screen, Navigator.NoOp, unavailableContent)
}

@Composable
private fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  unavailableContent: (@Composable () -> Unit)? = null,
) {
  val circuit = LocalCircuitOwner.current

  val containerViewModel = viewModel<Continuity>()
  val presenter =
    remember(containerViewModel) {
      containerViewModel.presenterForScreen(screen) {
        @Suppress("UNCHECKED_CAST")
        circuit.presenter(screen, navigator) as Presenter<Any, Any>?
      }
    }
  val activity = LocalContext.current.findActivity()
  remember(screen) {
    object : RememberObserver {
      override fun onAbandoned() = disposeIfNotChangingConfiguration()

      override fun onForgotten() = disposeIfNotChangingConfiguration()

      override fun onRemembered() {
        // Do nothing
      }

      fun disposeIfNotChangingConfiguration() {
        if (activity?.isChangingConfigurations != true) {
          containerViewModel.removePresenterForScreen<Any, Any>(screen)
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST") val ui = circuit.ui(screen, navigator) as Ui<Any, Any>?

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

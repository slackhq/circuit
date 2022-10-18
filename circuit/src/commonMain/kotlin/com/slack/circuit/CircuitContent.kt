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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
public fun CircuitContent(
  screen: Screen,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuitConfig.onUnavailableContent,
) {
  CircuitContent(screen, Navigator.NoOp, circuitConfig, unavailableContent)
}

@Composable
public fun CircuitContent(
  screen: Screen,
  onNavEvent: (event: NavEvent) -> Unit,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
  unavailableContent: (@Composable (screen: Screen) -> Unit)? = circuitConfig.onUnavailableContent,
) {
  val navigator =
    remember(onNavEvent) {
      object : Navigator {
        override fun goTo(screen: Screen) {
          onNavEvent(GoToNavEvent(screen))
        }

        override fun pop(): Screen? {
          onNavEvent(PopNavEvent)
          return null
        }
      }
    }
  CircuitContent(screen, navigator, circuitConfig, unavailableContent)
}

@Composable
internal fun CircuitContent(
  screen: Screen,
  navigator: Navigator,
  circuitConfig: CircuitConfig,
  unavailableContent: (@Composable (screen: Screen) -> Unit)?,
) {
  val screenUi = circuitConfig.ui(screen)

  @Suppress("UNCHECKED_CAST")
  val presenter = circuitConfig.presenter(screen, navigator) as Presenter<CircuitUiState>?

  if (screenUi != null && presenter != null) {
    val eventListener = circuitConfig.eventListenerFactory?.create(screen) ?: EventListener.NONE
    @Suppress("UNCHECKED_CAST")
    CircuitContent(eventListener, presenter, screenUi.ui as Ui<CircuitUiState>)
  } else if (unavailableContent != null) {
    unavailableContent(screen)
  } else {
    error("Could not render screen $screen")
  }
}

@Composable
private fun <UiState : CircuitUiState> CircuitContent(
  eventListener: EventListener,
  presenter: Presenter<UiState>,
  ui: Ui<UiState>,
) {
  val state = presenter.present()
  val stateChanges = remember(presenter, ui) { snapshotFlow { state }.distinctUntilChanged() }
  LaunchedEffect(stateChanges) { stateChanges.collect { eventListener.onState(it) } }
  ui.Content(state)
}

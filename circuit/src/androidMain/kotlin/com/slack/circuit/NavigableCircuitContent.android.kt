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
public fun NavigableCircuitContent(
  navigator: Navigator,
  backstack: SaveableBackStack,
  modifier: Modifier = Modifier,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
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
    circuitConfig = circuitConfig,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

@Composable
public fun BasicNavigableCircuitContent(
  navigator: Navigator,
  backstack: SaveableBackStack,
  providedValues: Map<out BackStack.Record, ProvidedValues>,
  modifier: Modifier = Modifier,
  circuitConfig: CircuitConfig = LocalCircuitOwner.current,
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
            CircuitContent(screen, navigator, circuitConfig) { unavailableRoute(routeName) }
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

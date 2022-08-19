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
import com.slack.circuit.backstack.NavigatorDefaults
import com.slack.circuit.backstack.NavigatorRouteDecoration
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack

/**
 * Returns a new [Navigator] that is backed by this [Circuit] instance.
 *
 * @param contentContainer The [ContentContainer] to render into.
 * @param onRootPop The [OnPopHandler] to handle root [Navigator.pop] calls.
 */
// TODO should we try to make a composable rememberNavigator() function here too?
fun Circuit.navigator(contentContainer: ContentContainer, onRootPop: OnPopHandler?): Navigator {
  return CircuitNavigator(this, contentContainer, onRootPop)
}

internal class CircuitNavigator(
  val circuit: Circuit,
  val contentContainer: ContentContainer,
  val onRootPop: OnPopHandler?,
) : Navigator {

  override fun goTo(screen: Screen) {
    // Headless? - MutableStateFlow and launch
    // Testing - molecule to convert it to a Flow, then plumb into Turbine and treat it as a
    // coroutines test. Don't ever use Dispatchers default/io in tests
    contentContainer.render {
      val backstack = rememberSaveableBackStack { push(screen) }
      val goTo: (Screen) -> Unit = { screen -> backstack.push(screen) }
      val navigator =
        object : Navigator {
          override fun goTo(screen: Screen) {
            goTo(screen)
          }

          override fun pop(): Screen? {
            return if (!backstack.isAtRoot) {
              backstack.pop()?.screen
            } else {
              this@CircuitNavigator.pop()
              null
            }
          }
        }
      FactoryNavigator(circuit, backstack, navigator, contentContainer)
    }
  }

  override fun pop(): Screen? {
    onRootPop?.onPop()
    return null
  }
}

@JvmInline
private value class UiStateRenderer<UiState, UiEvent : Any>(val ui: Ui<UiState, UiEvent>) :
  StateRenderer<UiState, UiEvent> where UiState : Any, UiState : Parcelable {
  @Composable
  override fun render(state: UiState, uiEvents: (UiEvent) -> Unit) {
    ui.render(state, uiEvents)
  }
}

@Composable
internal fun <R : BackStack.Record> FactoryNavigator(
  circuit: Circuit,
  backStack: BackStack<R>,
  navigator: Navigator,
  container: ContentContainer,
  modifier: Modifier = Modifier,
  enableBackHandler: Boolean = true,
  providedValues: Map<R, ProvidedValues> = providedValuesForBackStack(backStack),
  decoration: NavigatorRouteDecoration = NavigatorDefaults.DefaultDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  BackHandler(enabled = enableBackHandler && !backStack.isAtRoot) { backStack.pop() }

  BasicFactoryNavigator(
    circuit = circuit,
    backStack = backStack,
    navigator = navigator,
    container = container,
    providedValues = providedValues,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

@Composable
internal fun <R : BackStack.Record> BasicFactoryNavigator(
  circuit: Circuit,
  backStack: BackStack<R>,
  navigator: Navigator,
  container: ContentContainer,
  providedValues: Map<R, ProvidedValues>,
  modifier: Modifier = Modifier,
  decoration: NavigatorRouteDecoration = NavigatorDefaults.EmptyDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  // TODO remember { mutableStatOf(circuit) } ?

  val activeContentProviders = buildList {
    for (record in backStack) {
      val provider =
        key(record.key) {
          val routeName = record.route
          // TODO fix this
          val screen = (record as SaveableBackStack.Record).screen

          @Suppress("UNCHECKED_CAST") val ui = circuit.ui(screen, container) as Ui<Parcelable, Any>?

          @Suppress("UNCHECKED_CAST")
          val presenter = circuit.presenter(screen, navigator) as Presenter<Parcelable, Any>?

          val currentRender: (@Composable (R) -> Unit) =
            if (presenter != null && ui != null) {
              { presenter.present(UiStateRenderer(ui)) }
            } else {
              { unavailableRoute(routeName) }
            }

          val currentRouteContent by rememberUpdatedState(currentRender)
          val currentRecord by rememberUpdatedState(record)
          remember { movableContentOf { currentRouteContent(currentRecord) } }
        }
      add(record to provider)
    }
  }

  if (backStack.size > 0) {
    @Suppress("SpreadOperator")
    decoration.DecoratedContent(activeContentProviders.first(), backStack.size, modifier) {
      (record, provider) ->
      val values = providedValues[record]?.provideValues()
      val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }
      CompositionLocalProvider(*providedLocals) { provider.invoke() }
    }
  }
}

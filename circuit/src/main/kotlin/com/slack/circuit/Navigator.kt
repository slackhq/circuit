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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.slack.circuit.backstack.BackStack
import com.slack.circuit.backstack.NavigatorDefaults
import com.slack.circuit.backstack.NavigatorRouteDecoration
import com.slack.circuit.backstack.ProvidedValues
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.isAtRoot
import com.slack.circuit.backstack.providedValuesForBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

fun interface OnPopHandler {
  fun onPop()
}

/** TODO doc this */
interface Navigator {
  fun goTo(screen: Screen)

  fun pop()

  fun interface Factory<T : Navigator> {
    fun create(renderer: ContentContainer, onRootPop: OnPopHandler?): T
  }
}

/**
 * A simple rendering abstraction over a `@Composable () -> Unit`.
 *
 * This allows for any host container that can render composable functions to be used with a given
 * [Navigator.Factory.create] call, such as [ComponentActivity] and [ComposeView].
 */
fun interface ContentContainer {
  fun render(content: @Composable () -> Unit)
}

fun ComposeView.asContentContainer() = ContentContainer(::setContent)

fun ComponentActivity.asContentContainer() = ContentContainer { content ->
  setContent(content = content)
}

class NavigatorImpl
@AssistedInject
constructor(
  @Assisted val container: ContentContainer,
  @Assisted val onRootPop: OnPopHandler?,
  // TODO do we ever handle precedence/order here?
  val presenterFactories: @JvmSuppressWildcards Set<PresenterFactory>,
  val uiFactories: @JvmSuppressWildcards Set<ScreenViewFactory>,
) : Navigator {

  @AssistedFactory fun interface Factory : Navigator.Factory<NavigatorImpl>

  override fun goTo(screen: Screen) {
    // Headless? - MutableStateFlow and launch
    // Testing - molecule to convert it to a Flow, then plumb into Turbine and treat it as a
    // coroutines test. Don't ever use Dispatchers default/io in tests
    container.render {
      val backstack = rememberSaveableBackStack { push(screen) }
      val goTo: (Screen) -> Unit = { screen -> backstack.push(screen) }
      val navigator =
        object : Navigator {
          override fun goTo(screen: Screen) {
            goTo(screen)
          }

          override fun pop() {
            if (!backstack.isAtRoot) {
              backstack.pop()
            } else {
              this@NavigatorImpl.pop()
            }
          }
        }
      FactoryNavigator(backstack, presenterFactories, uiFactories, navigator, container)
    }
  }

  override fun pop() {
    onRootPop?.onPop()
  }
}

@Composable
fun <R : BackStack.Record> FactoryNavigator(
  backStack: BackStack<R>,
  presenterFactories: Set<PresenterFactory>,
  uiFactories: Set<ScreenViewFactory>,
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
    backStack = backStack,
    presenterFactories = presenterFactories,
    uiFactories = uiFactories,
    navigator = navigator,
    container = container,
    providedValues = providedValues,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
  )
}

@Composable
fun <R : BackStack.Record> BasicFactoryNavigator(
  backStack: BackStack<R>,
  presenterFactories: Set<PresenterFactory>,
  uiFactories: Set<ScreenViewFactory>,
  navigator: Navigator,
  container: ContentContainer,
  providedValues: Map<R, ProvidedValues>,
  modifier: Modifier = Modifier,
  decoration: NavigatorRouteDecoration = NavigatorDefaults.EmptyDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
) {
  val currentPresenters by rememberUpdatedState(presenterFactories)
  val currentUis by rememberUpdatedState(uiFactories)

  val activeContentProviders = buildList {
    for (record in backStack) {
      val provider =
        key(record.key) {
          val routeName = record.route
          // TODO fix this
          val screen = (record as SaveableBackStack.Record).screen

          @Suppress("UNCHECKED_CAST")
          val ui =
            currentUis.firstNotNullOfOrNull { it.createView(screen, container) }?.ui
              as Ui<Parcelable, Any>?

          @Suppress("UNCHECKED_CAST")
          val presenter =
            currentPresenters.firstNotNullOfOrNull {
              // TODO fix exposition of navigator/backstack
              it.create(screen, navigator)
            } as Presenter<Parcelable, Any>?

          val currentRender: (@Composable (R) -> Unit) =
            if (presenter != null && ui != null) {
              {
                val channel = remember(presenter, ui) { Channel<Any>(BUFFERED) }
                val eventsFlow = remember(channel) { channel.receiveAsFlow() }
                // TODO where does rememberSaveable fit here??
                val state = presenter.present(eventsFlow)
                ui.render(state) { event -> channel.trySend(event) }
              }
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

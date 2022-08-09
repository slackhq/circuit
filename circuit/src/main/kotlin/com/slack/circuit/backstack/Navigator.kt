/*
 * Copyright (C) 2022 Adam Powell
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
package com.slack.circuit.backstack

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** [DslMarker] for the [NavigationBuilder] DSL. */
@DslMarker annotation class NavigationBuilderDsl

/** Declarative builder scope for navigation routes; used with [Navigator]. */
@NavigationBuilderDsl
interface NavigationBuilder<R> {
  fun route(route: String, content: @Composable (R) -> Unit)
}

/** Presentation logic for currently visible routes of a [Navigator]. */
@Stable
interface NavigatorRouteDecoration {
  @Composable
  fun <T> DecoratedContent(
    arg: T,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit
  )
}

private class NavigationMap<R> : NavigationBuilder<R> {
  val routes = mutableMapOf<String, @Composable (R) -> Unit>()

  override fun route(route: String, content: @Composable (R) -> Unit) {
    routes[route] = content
  }
}

/** Default values and common alternatives used by the [Navigator] family of composables. */
object NavigatorDefaults {

  /** The default [NavigatorRouteDecoration] used by [Navigator]. */
  object DefaultDecoration : NavigatorRouteDecoration {
    @Composable
    override fun <T> DecoratedContent(
      arg: T,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit
    ) {
      Crossfade(arg, modifier, content = content)
    }
  }

  /**
   * An empty [NavigatorRouteDecoration] that emits the content with no surrounding decoration or
   * logic.
   */
  object EmptyDecoration : NavigatorRouteDecoration {
    @Composable
    override fun <T> DecoratedContent(
      arg: T,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit
    ) {
      content(arg)
    }
  }

  /**
   * Bright ugly error text telling a developer they didn't provide a route that a [BackStack] asked
   * for.
   */
  val UnavailableRoute: @Composable (String) -> Unit = { route ->
    BasicText(
      "Route not available: $route",
      Modifier.background(Color.Red),
      style = TextStyle(color = Color.Yellow)
    )
  }
}

@Composable
fun <R : BackStack.Record> Navigator(
  backStack: BackStack<R>,
  modifier: Modifier = Modifier,
  enableBackHandler: Boolean = true,
  providedValues: Map<R, ProvidedValues> = providedValuesForBackStack(backStack),
  decoration: NavigatorRouteDecoration = NavigatorDefaults.DefaultDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
  navigationBuilder: NavigationBuilder<R>.() -> Unit
) {
  BackHandler(enabled = enableBackHandler && !backStack.isAtRoot) { backStack.pop() }

  BasicNavigator(
    backStack = backStack,
    providedValues = providedValues,
    modifier = modifier,
    decoration = decoration,
    unavailableRoute = unavailableRoute,
    navigationBuilder = navigationBuilder
  )
}

@Composable
fun <R : BackStack.Record> BasicNavigator(
  backStack: BackStack<R>,
  providedValues: Map<R, ProvidedValues>,
  modifier: Modifier = Modifier,
  decoration: NavigatorRouteDecoration = NavigatorDefaults.EmptyDecoration,
  unavailableRoute: @Composable (String) -> Unit = NavigatorDefaults.UnavailableRoute,
  navigationBuilder: NavigationBuilder<R>.() -> Unit
) {
  val currentBuilder by rememberUpdatedState(navigationBuilder)
  val routes by remember { derivedStateOf { NavigationMap<R>().apply(currentBuilder) } }

  val activeContentProviders = buildList {
    for (record in backStack) {
      val provider =
        key(record.key) {
          val routeName = record.route
          val currentRouteContent by
            rememberUpdatedState(routes.routes[routeName] ?: { unavailableRoute(routeName) })
          val currentRecord by rememberUpdatedState(record)
          remember { movableContentOf { currentRouteContent(currentRecord) } }
        }
      add(record to provider)
    }
  }

  @Suppress("SpreadOperator")
  decoration.DecoratedContent(activeContentProviders.first(), backStack.size, modifier) {
    (record, provider) ->
    val values = providedValues[record]?.provideValues()
    val providedLocals = remember(values) { values?.toTypedArray() ?: emptyArray() }
    CompositionLocalProvider(*providedLocals) { provider.invoke() }
  }
}

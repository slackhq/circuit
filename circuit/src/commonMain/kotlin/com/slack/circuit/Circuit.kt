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

import androidx.compose.runtime.Immutable

/**
 * Circuit adapts [presenterFactories][PresenterFactory] to their corresponding renderable
 * [uiFactories][ScreenViewFactory] using [screens][Screen]. Create instances using [the Builder]
 * [Builder] and create new [CircuitContent] with it to run presenter/UI pairings.
 *
 * ## Construction
 *
 * Construction of [Circuit] instances is done using [the Builder] [Builder].
 *
 * ```kotlin
 * val circuit = Circuit.Builder()
 *     .addUiFactory(AddFavoritesScreenViewFactory()
 *     .addPresenterFactory(AddFavoritesPresenterFactory()
 *     .build()
 * ```
 *
 * ## Usage
 *
 * These instances can then be used with the composable [CircuitContent] functions to run
 * presenter/UI pairings for individual [screens][Screen].
 *
 * Circuit instances are consumed within these composable functions via the [CircuitProvider]
 * CompositionLocalProvider.
 *
 * ```kotlin
 * CircuitProvider(circuit) {
 *   CircuitContent(AddFavoritesScreen())
 * }
 * ```
 *
 * If using navigation, use `NavigableCircuitContent` instead.
 *
 * ```kotlin
 * val backstack = rememberSaveableBackStack { push(AddFavoritesScreen()) }
 * val navigator = rememberCircuitNavigator(backstack, ::onBackPressed)
 * CircuitProvider(circuit) {
 *   NavigableCircuitContent(navigator, backstack)
 * }
 * ```
 *
 * @see `rememberCircuitNavigator`
 * @see `NavigableCircuitContent`
 * @see CircuitContent
 */
@Immutable
public class Circuit private constructor(builder: Builder) {
  private val uiFactories: List<ScreenViewFactory> = builder.uiFactories.toList()
  private val presenterFactories: List<PresenterFactory> = builder.presenterFactories.toList()

  public fun presenter(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    return nextPresenter(null, screen, navigator)
  }

  public fun nextPresenter(
    skipPast: PresenterFactory?,
    screen: Screen,
    navigator: Navigator
  ): Presenter<*, *>? {
    val start = presenterFactories.indexOf(skipPast) + 1
    // TODO use the normal range syntax in 1.7.20
    (start until presenterFactories.size).forEach { i ->
      val presenter = presenterFactories[i].create(screen, navigator)
      if (presenter != null) {
        return presenter
      }
    }

    return null
  }

  public fun ui(screen: Screen): Ui<*, *>? {
    return nextUi(null, screen)
  }

  public fun nextUi(skipPast: ScreenViewFactory?, screen: Screen): Ui<*, *>? {
    val start = uiFactories.indexOf(skipPast) + 1
    // TODO use the normal range syntax in 1.7.20
    (start until uiFactories.size).forEach { i ->
      val ui = uiFactories[i].createView(screen)
      if (ui != null) {
        return ui.ui
      }
    }

    return null
  }

  public fun newBuilder(): Builder = Builder(this)

  public class Builder constructor() {
    public val uiFactories: MutableList<ScreenViewFactory> = mutableListOf()
    public val presenterFactories: MutableList<PresenterFactory> = mutableListOf()

    internal constructor(circuit: Circuit) : this() {
      uiFactories.addAll(circuit.uiFactories)
      presenterFactories.addAll(circuit.presenterFactories)
    }

    public fun addUiFactory(factory: ScreenViewFactory): Builder = apply {
      uiFactories.add(factory)
    }

    public fun addUiFactory(vararg factory: ScreenViewFactory): Builder = apply {
      for (f in factory) {
        uiFactories.add(f)
      }
    }

    public fun addUiFactories(factories: Iterable<ScreenViewFactory>): Builder = apply {
      uiFactories.addAll(factories)
    }

    public fun addPresenterFactory(factory: PresenterFactory): Builder = apply {
      presenterFactories.add(factory)
    }

    public fun addPresenterFactory(vararg factory: PresenterFactory): Builder = apply {
      for (f in factory) {
        presenterFactories.add(f)
      }
    }

    public fun addPresenterFactories(factories: Iterable<PresenterFactory>): Builder = apply {
      presenterFactories.addAll(factories)
    }

    public fun build(): Circuit {
      return Circuit(this)
    }
  }
}

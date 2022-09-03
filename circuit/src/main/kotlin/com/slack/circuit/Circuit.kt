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
 * If using navigation, use [NavigableCircuitContent] instead.
 *
 * ```kotlin
 * val backstack = rememberSaveableBackStack { push(AddFavoritesScreen()) }
 * val navigator = rememberCircuitNavigator(backstack, ::onBackPressed)
 * CircuitProvider(circuit) {
 *   NavigableCircuitContent(navigator, backstack)
 * }
 * ```
 *
 * @see rememberCircuitNavigator
 * @see CircuitContent
 * @see NavigableCircuitContent
 */
@Immutable
class Circuit private constructor(builder: Builder) {
  private val uiFactories: List<ScreenViewFactory> = builder.uiFactories.toList()
  private val presenterFactories: List<PresenterFactory> = builder.presenterFactories.toList()

  fun presenter(screen: Screen): Presenter<*, *>? {
    return nextPresenter(null, screen)
  }

  fun nextPresenter(skipPast: PresenterFactory?, screen: Screen): Presenter<*, *>? {
    val start = presenterFactories.indexOf(skipPast) + 1
    for (i in start until presenterFactories.size) {
      val presenter = presenterFactories[i].create(screen, this)
      if (presenter != null) {
        return presenter
      }
    }

    return null
  }

  fun ui(screen: Screen): Ui<*, *>? {
    return nextUi(null, screen)
  }

  fun nextUi(skipPast: ScreenViewFactory?, screen: Screen): Ui<*, *>? {
    val start = uiFactories.indexOf(skipPast) + 1
    for (i in start until uiFactories.size) {
      val ui = uiFactories[i].createView(screen)
      if (ui != null) {
        return ui.ui
      }
    }

    return null
  }

  fun newBuilder() = Builder(this)

  class Builder constructor() {
    val uiFactories = mutableListOf<ScreenViewFactory>()
    val presenterFactories = mutableListOf<PresenterFactory>()

    internal constructor(circuit: Circuit) : this() {
      uiFactories.addAll(circuit.uiFactories)
      presenterFactories.addAll(circuit.presenterFactories)
    }

    fun addUiFactory(factory: ScreenViewFactory) = apply { uiFactories.add(factory) }

    fun addUiFactory(vararg factory: ScreenViewFactory) = apply {
      for (f in factory) {
        uiFactories.add(f)
      }
    }

    fun addUiFactories(factories: Iterable<ScreenViewFactory>) = apply {
      uiFactories.addAll(factories)
    }

    fun addPresenterFactory(factory: PresenterFactory) = apply { presenterFactories.add(factory) }

    fun addPresenterFactory(vararg factory: PresenterFactory) = apply {
      for (f in factory) {
        presenterFactories.add(f)
      }
    }

    fun addPresenterFactories(factories: Iterable<PresenterFactory>) = apply {
      presenterFactories.addAll(factories)
    }

    fun build(): Circuit {
      return Circuit(this)
    }
  }
}

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
import androidx.compose.runtime.Immutable

/**
 * Circuit adapts [presenter factories][Presenter.Factory] to their corresponding renderable [ui
 * factories][Ui.Factory] using [screens][Screen]. Create instances using [the Builder] [Builder]
 * and create new [CircuitContent] with it to run presenter/UI pairings.
 *
 * ## Construction
 *
 * Construction of [Circuit] instances is done using [the Builder] [Builder].
 *
 * ```kotlin
 * val circuit = Circuit.Builder()
 *     .addUiFactory(AddFavoritesUiFactory()
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
  private val uiFactories: List<Ui.Factory> = builder.uiFactories.toList()
  private val presenterFactories: List<Presenter.Factory> = builder.presenterFactories.toList()
  val onUnavailableContent: (@Composable (screen: Screen) -> Unit)? = builder.onUnavailableContent

  fun presenter(screen: Screen, navigator: Navigator): Presenter<*, *>? {
    return nextPresenter(null, screen, navigator)
  }

  fun nextPresenter(
    skipPast: Presenter.Factory?,
    screen: Screen,
    navigator: Navigator
  ): Presenter<*, *>? {
    val start = presenterFactories.indexOf(skipPast) + 1
    for (i in start until presenterFactories.size) {
      val presenter = presenterFactories[i].create(screen, navigator)
      if (presenter != null) {
        return presenter
      }
    }

    return null
  }

  fun ui(screen: Screen): ScreenUi? {
    return nextUi(null, screen)
  }

  fun nextUi(skipPast: Ui.Factory?, screen: Screen): ScreenUi? {
    val start = uiFactories.indexOf(skipPast) + 1
    for (i in start until uiFactories.size) {
      val ui = uiFactories[i].create(screen)
      if (ui != null) {
        return ui
      }
    }

    return null
  }

  fun newBuilder() = Builder(this)

  class Builder constructor() {
    val uiFactories = mutableListOf<Ui.Factory>()
    val presenterFactories = mutableListOf<Presenter.Factory>()
    var onUnavailableContent: (@Composable (screen: Screen) -> Unit)? = null
      private set

    internal constructor(circuit: Circuit) : this() {
      uiFactories.addAll(circuit.uiFactories)
      presenterFactories.addAll(circuit.presenterFactories)
    }

    fun addUiFactory(factory: Ui.Factory) = apply { uiFactories.add(factory) }

    fun addUiFactory(vararg factory: Ui.Factory) = apply {
      for (f in factory) {
        uiFactories.add(f)
      }
    }

    fun addUiFactories(factories: Iterable<Ui.Factory>) = apply { uiFactories.addAll(factories) }

    fun addPresenterFactory(factory: Presenter.Factory) = apply { presenterFactories.add(factory) }

    fun addPresenterFactory(vararg factory: Presenter.Factory) = apply {
      for (f in factory) {
        presenterFactories.add(f)
      }
    }

    fun addPresenterFactories(factories: Iterable<Presenter.Factory>) = apply {
      presenterFactories.addAll(factories)
    }

    fun setOnUnavailableContentCallback(callback: @Composable (screen: Screen) -> Unit) = apply {
      onUnavailableContent = callback
    }

    fun build(): Circuit {
      return Circuit(this)
    }
  }
}

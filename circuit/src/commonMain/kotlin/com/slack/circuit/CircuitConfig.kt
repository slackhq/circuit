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
 * [CircuitConfig] adapts [presenter factories][Presenter.Factory] to their corresponding renderable
 * [ui factories][Ui.Factory] using [screens][Screen]. Create instances using [the Builder]
 * [Builder] and create new [CircuitContent] with it to run presenter/UI pairings.
 *
 * ## Construction
 *
 * Construction of [CircuitConfig] instances is done using [the Builder] [Builder].
 *
 * ```kotlin
 * val circuitConfig = CircuitConfig.Builder()
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
 * Circuit instances are consumed within these composable functions via the
 * [CircuitCompositionLocals] CompositionLocalProvider.
 *
 * ```kotlin
 * CircuitCompositionLocals(circuitConfig) {
 *   CircuitContent(AddFavoritesScreen())
 * }
 * ```
 *
 * If using navigation, use `NavigableCircuitContent` instead.
 *
 * ```kotlin
 * val backstack = rememberSaveableBackStack { push(AddFavoritesScreen()) }
 * val navigator = rememberCircuitNavigator(backstack, ::onBackPressed)
 * CircuitCompositionLocals(circuitConfig) {
 *   NavigableCircuitContent(navigator, backstack)
 * }
 * ```
 *
 * @see rememberCircuitNavigator
 * @see CircuitContent
 * @see NavigableCircuitContent
 */
@Immutable
public class CircuitConfig private constructor(builder: Builder) {
  private val uiFactories: List<Ui.Factory> = builder.uiFactories.toList()
  private val presenterFactories: List<Presenter.Factory> = builder.presenterFactories.toList()
  public val onUnavailableContent: (@Composable (screen: Screen) -> Unit)? =
    builder.onUnavailableContent

  public fun presenter(screen: Screen, navigator: Navigator): Presenter<*>? {
    return nextPresenter(null, screen, navigator)
  }

  public fun nextPresenter(
    skipPast: Presenter.Factory?,
    screen: Screen,
    navigator: Navigator
  ): Presenter<*>? {
    val start = presenterFactories.indexOf(skipPast) + 1
    for (i in start until presenterFactories.size) {
      val presenter = presenterFactories[i].create(screen, navigator, this)
      if (presenter != null) {
        return presenter
      }
    }

    return null
  }

  public fun ui(screen: Screen): ScreenUi? {
    return nextUi(null, screen)
  }

  public fun nextUi(skipPast: Ui.Factory?, screen: Screen): ScreenUi? {
    val start = uiFactories.indexOf(skipPast) + 1
    for (i in start until uiFactories.size) {
      val ui = uiFactories[i].create(screen, this)
      if (ui != null) {
        return ui
      }
    }

    return null
  }

  public fun newBuilder(): Builder = Builder(this)

  public class Builder constructor() {
    public val uiFactories: MutableList<Ui.Factory> = mutableListOf<Ui.Factory>()
    public val presenterFactories: MutableList<Presenter.Factory> =
      mutableListOf<Presenter.Factory>()
    public var onUnavailableContent: (@Composable (screen: Screen) -> Unit)? = null
      private set

    internal constructor(circuitConfig: CircuitConfig) : this() {
      uiFactories.addAll(circuitConfig.uiFactories)
      presenterFactories.addAll(circuitConfig.presenterFactories)
    }

    public fun addUiFactory(factory: Ui.Factory): Builder = apply { uiFactories.add(factory) }

    public fun addUiFactory(vararg factory: Ui.Factory): Builder = apply {
      for (f in factory) {
        uiFactories.add(f)
      }
    }

    public fun addUiFactories(factories: Iterable<Ui.Factory>): Builder = apply {
      uiFactories.addAll(factories)
    }

    public fun addPresenterFactory(factory: Presenter.Factory): Builder = apply {
      presenterFactories.add(factory)
    }

    public fun addPresenterFactory(vararg factory: Presenter.Factory): Builder = apply {
      for (f in factory) {
        presenterFactories.add(f)
      }
    }

    public fun addPresenterFactories(factories: Iterable<Presenter.Factory>): Builder = apply {
      presenterFactories.addAll(factories)
    }

    public fun setOnUnavailableContent(content: @Composable (screen: Screen) -> Unit): Builder =
      apply {
        onUnavailableContent = content
      }

    public fun build(): CircuitConfig {
      return CircuitConfig(this)
    }
  }
}

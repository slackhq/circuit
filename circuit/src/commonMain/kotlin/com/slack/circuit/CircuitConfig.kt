// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.slack.circuit.runtime.Screen

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
 *     .addPresenterFactory(AddFavoritesPresenterFactory())
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
 * @see CircuitContent
 * @see `rememberCircuitNavigator`
 * @see `NavigableCircuitContent`
 */
@Immutable
public class CircuitConfig private constructor(builder: Builder) {
  private val uiFactories: List<Ui.Factory> = builder.uiFactories.toList()
  private val presenterFactories: List<Presenter.Factory> = builder.presenterFactories.toList()
  public val onUnavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    builder.onUnavailableContent
  internal val eventListenerFactory: EventListener.Factory? = builder.eventListenerFactory

  public fun presenter(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext = CircuitContext(null, this)
  ): Presenter<*>? {
    return nextPresenter(null, screen, navigator, context)
  }

  public fun nextPresenter(
    skipPast: Presenter.Factory?,
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext
  ): Presenter<*>? {
    val start = presenterFactories.indexOf(skipPast) + 1
    for (i in start until presenterFactories.size) {
      val presenter = presenterFactories[i].create(screen, navigator, context)
      if (presenter != null) {
        return presenter
      }
    }

    return null
  }

  public fun ui(screen: Screen, context: CircuitContext = CircuitContext(null, this)): Ui<*>? {
    return nextUi(null, screen, context)
  }

  public fun nextUi(skipPast: Ui.Factory?, screen: Screen, context: CircuitContext): Ui<*>? {
    val start = uiFactories.indexOf(skipPast) + 1
    for (i in start until uiFactories.size) {
      val ui = uiFactories[i].create(screen, context)
      if (ui != null) {
        return ui
      }
    }

    return null
  }

  public fun newBuilder(): Builder = Builder(this)

  public class Builder constructor() {
    public val uiFactories: MutableList<Ui.Factory> = mutableListOf()
    public val presenterFactories: MutableList<Presenter.Factory> = mutableListOf()
    public var onUnavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
      UnavailableContent
      private set
    public var eventListenerFactory: EventListener.Factory? = null
      private set

    internal constructor(circuitConfig: CircuitConfig) : this() {
      uiFactories.addAll(circuitConfig.uiFactories)
      presenterFactories.addAll(circuitConfig.presenterFactories)
      eventListenerFactory = circuitConfig.eventListenerFactory
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

    public fun setOnUnavailableContent(
      content: @Composable (screen: Screen, modifier: Modifier) -> Unit
    ): Builder = apply { onUnavailableContent = content }

    public fun eventListenerFactory(factory: EventListener.Factory): Builder = apply {
      eventListenerFactory = factory
    }

    public fun build(): CircuitConfig {
      return CircuitConfig(this)
    }
  }
}

private val UnavailableContent: @Composable (screen: Screen, modifier: Modifier) -> Unit =
  { screen, modifier ->
    BasicText(
      "Route not available: ${screen.javaClass.name}",
      modifier.background(Color.Red),
      style = TextStyle(color = Color.Yellow)
    )
  }

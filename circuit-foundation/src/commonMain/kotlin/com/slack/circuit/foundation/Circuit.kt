// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.StaticScreen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap

/**
 * [Circuit] adapts [presenter factories][Presenter.Factory] to their corresponding
 * [ui factories][Ui.Factory] using [screens][Screen]. Create instances using [the Builder]
 * [Builder] and create new [CircuitContent] with it to run presenter/UI pairings.
 *
 * ## Construction
 *
 * Construction of [Circuit] instances is done using [the Builder] [Builder].
 *
 * ```kotlin
 * val circuit = Circuit.Builder()
 *     .addUiFactory(AddFavoritesUiFactory())
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
 * CircuitCompositionLocals(circuit) {
 *   CircuitContent(AddFavoritesScreen())
 * }
 * ```
 *
 * If using navigation, use `NavigableCircuitContent` instead.
 *
 * ```kotlin
 * val backStack = rememberSaveableBackStack(root = HomeScreen)
 * val navigator = rememberCircuitNavigator(backstack, ::onBackPressed)
 * CircuitCompositionLocals(circuit) {
 *   NavigableCircuitContent(navigator, backstack)
 * }
 * ```
 *
 * @see CircuitContent
 * @see `rememberCircuitNavigator`
 * @see `NavigableCircuitContent`
 */
@Immutable
public class Circuit private constructor(builder: Builder) {
  private val uiFactories: List<Ui.Factory> = builder.uiFactories.toList()
  private val presenterFactories: List<Presenter.Factory> = builder.presenterFactories.toList()
  public val onUnavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
    builder.onUnavailableContent
  public val animatedScreenTransforms: ImmutableMap<KClass<Screen>, AnimatedScreenTransform> =
    builder.animatedScreenTransforms.toImmutableMap()
  public val animatedNavDecoratorFactory: AnimatedNavDecorator.Factory =
    builder.animatedNavDecoratorFactory
  public val defaultNavDecoration: NavDecoration =
    builder.defaultNavDecoration
      ?: AnimatedNavDecoration(
        animatedScreenTransforms = animatedScreenTransforms,
        decoratorFactory = animatedNavDecoratorFactory,
      )

  internal val eventListenerFactory: EventListener.Factory? = builder.eventListenerFactory

  /**
   * If enabled, presenters will be "paused" when inactive on the back stack and cache their last
   * emitted state.
   *
   * @see NonPausablePresenter
   * @see RecordLifecycle
   * @see presentWithLifecycle
   * @see pausableState
   */
  public val presentWithLifecycle: Boolean = builder.presentWithLifecycle

  @OptIn(InternalCircuitApi::class)
  public fun presenter(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext = CircuitContext(null).also { it.circuit = this },
  ): Presenter<*>? {
    return nextPresenter(null, screen, navigator, context)
  }

  public fun nextPresenter(
    skipPast: Presenter.Factory?,
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ): Presenter<*>? {
    val start = presenterFactories.indexOf(skipPast) + 1
    for (i in start until presenterFactories.size) {
      val presenter = presenterFactories[i].create(screen, navigator, context)
      if (presenter != null) {
        return presenter
      }
    }

    // If it's static, gracefully fall back and return a stateless presenter and assume this is a
    // UI-only screen. We still try giving other presenter factories
    if (screen is StaticScreen) {
      return statelessPresenter<CircuitUiState>()
    }

    return null
  }

  @OptIn(InternalCircuitApi::class)
  public fun ui(
    screen: Screen,
    context: CircuitContext = CircuitContext(null).also { it.circuit = this },
  ): Ui<*>? {
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

  public class Builder() {
    public val uiFactories: MutableList<Ui.Factory> = mutableListOf()
    public val presenterFactories: MutableList<Presenter.Factory> = mutableListOf()
    public val animatedScreenTransforms: MutableMap<KClass<Screen>, AnimatedScreenTransform> =
      mutableMapOf()

    public var onUnavailableContent: (@Composable (screen: Screen, modifier: Modifier) -> Unit) =
      UnavailableContent
      private set

    public var defaultNavDecoration: NavDecoration? = null
      private set

    public var eventListenerFactory: EventListener.Factory? = null
      private set

    public var animatedNavDecoratorFactory: AnimatedNavDecorator.Factory =
      NavigatorDefaults.DefaultDecoratorFactory
      private set

    public var presentWithLifecycle: Boolean = true
      private set

    internal constructor(circuit: Circuit) : this() {
      uiFactories.addAll(circuit.uiFactories)
      presenterFactories.addAll(circuit.presenterFactories)
      animatedScreenTransforms.putAll(circuit.animatedScreenTransforms)
      animatedNavDecoratorFactory = circuit.animatedNavDecoratorFactory
      eventListenerFactory = circuit.eventListenerFactory
      // Carry over a custom NavDecoration if one was provided, otherwise use AnimatedNavDecoration
      if (circuit.defaultNavDecoration !is AnimatedNavDecoration) {
        defaultNavDecoration = circuit.defaultNavDecoration
      }
    }

    public inline fun <reified S : Screen, UiState : CircuitUiState> addUi(
      crossinline content: @Composable (state: UiState, modifier: Modifier) -> Unit
    ): Builder = apply {
      addUiFactory { screen, _ ->
        if (screen is S) {
          ui<UiState> { state, modifier -> content(state, modifier) }
        } else {
          null
        }
      }
    }

    /**
     * Adds a static UI that does not require a presenter. This is useful for screens that do not
     * have computed state. They can either be stateless or use the given [screen][S] as an input
     * for its state.
     *
     * When this is used, no corresponding [Presenter] needs to be added. Internally, an inert one
     * is created for linking purposes but is never used.
     */
    public inline fun <reified S : Screen, UiState : CircuitUiState> addStaticUi(
      crossinline content: @Composable (screen: S, modifier: Modifier) -> Unit
    ): Builder = apply {
      addUiFactory { screen, _ ->
        if (screen is S) {
          ui<UiState> { _, modifier -> content(screen, modifier) }
        } else {
          null
        }
      }
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

    public inline fun <reified S : Screen, UiState : CircuitUiState> addPresenter(
      crossinline factory:
        (screen: S, navigator: Navigator, context: CircuitContext) -> Presenter<UiState>
    ): Builder = apply {
      addPresenterFactory { screen, navigator, context ->
        if (screen is S) {
          factory(screen, navigator, context)
        } else {
          null
        }
      }
    }

    public inline fun <reified S : Screen, UiState : CircuitUiState> addPresenter(
      presenter: Presenter<UiState>
    ): Builder = apply {
      addPresenterFactory { screen, _, _ ->
        if (screen is S) {
          presenter
        } else {
          null
        }
      }
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

    public fun setAnimatedNavDecoratorFactory(
      decoratorFactory: AnimatedNavDecorator.Factory
    ): Builder = apply { animatedNavDecoratorFactory = decoratorFactory }

    public fun addAnimatedScreenTransform(
      screen: KClass<Screen>,
      animatedNavigationTransform: AnimatedScreenTransform,
    ): Builder = apply { animatedScreenTransforms[screen] = animatedNavigationTransform }

    public fun addAnimatedScreenTransforms(
      vararg pairs: Pair<KClass<Screen>, AnimatedScreenTransform>
    ): Builder = apply { animatedScreenTransforms.putAll(pairs) }

    public fun addAnimatedScreenTransforms(
      transforms: Map<KClass<Screen>, AnimatedScreenTransform>
    ): Builder = apply { animatedScreenTransforms.putAll(transforms) }

    public fun setOnUnavailableContent(
      content: @Composable (screen: Screen, modifier: Modifier) -> Unit
    ): Builder = apply { onUnavailableContent = content }

    public fun setDefaultNavDecoration(decoration: NavDecoration?): Builder = apply {
      defaultNavDecoration = decoration
    }

    public fun eventListenerFactory(factory: EventListener.Factory): Builder = apply {
      eventListenerFactory = factory
    }

    public fun presentWithLifecycle(enable: Boolean = true): Builder = apply {
      presentWithLifecycle = enable
    }

    public fun build(): Circuit {
      return Circuit(this)
    }
  }
}

private val UnavailableContent: @Composable (screen: Screen, modifier: Modifier) -> Unit =
  { screen, modifier ->
    if (LocalInspectionMode.current) {
      // In previews/tests, use a plainer placeholder
      BasicText(
        text = "⚡️CircuitScreen(${screen::class.simpleName})",
        modifier = modifier.background(Color.Gray),
        style = TextStyle(color = Color.Black),
      )
    } else {
      BasicText(
        text = "Route not available: $screen",
        modifier = modifier.background(Color.Red),
        style = TextStyle(color = Color.Yellow),
      )
    }
  }

/** The [Circuit] used in this context. */
public var CircuitContext.circuit: Circuit
  get() = checkNotNull(tag())
  set(value) {
    putTag(value)
  }

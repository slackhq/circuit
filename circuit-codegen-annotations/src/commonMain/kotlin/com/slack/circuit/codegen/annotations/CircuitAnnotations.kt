// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen.annotations

import androidx.compose.runtime.Composable
import com.slack.circuit.CircuitConfig
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.Ui
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.reflect.KClass

/**
 * This annotation is used to mark a presenter class or function for code generation. When
 * annotated, the corresponding factory will be generated and keyed with the defined [screen]
 * .
 *
 * The generated factories are then contributed to Anvil via [ContributesMultibinding] and scoped
 * with the provided [scope] key.
 *
 * ## Classes
 *
 * [Presenter] classes can be annotated and have their corresponding [Presenter.Factory] class
 * generated for them.
 *
 * **Presenter**
 * ```kotlin
 * @CircuitPresenter(HomeScreen::class, AppScope::class)
 * class HomePresenter @Inject constructor(...) : Presenter<HomeState> { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomePresenterFactory @Inject constructor() : Presenter.Factory { ... }
 * ```
 *
 * ## Functions
 *
 * Simple functions can be annotated and have a corresponding [Presenter.Factory] generated. This is
 * primarily useful for simple cases where a class is just technical tedium.
 *
 * **Requirements**
 * - Presenter functions _must_ return a [CircuitUiState] type.
 * - Presenter functions _must_ be [Composable].
 *
 * **Presenter**
 * ```kotlin
 * @CircuitPresenter(HomeScreen::class, AppScope::class)
 * @Composable
 * fun HomePresenter(): HomeState { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomePresenterFactory @Inject constructor() : Presenter.Factory { ... }
 * ```
 *
 * ## Assisted injection
 *
 * Any type that is offered in [Presenter.Factory] can be offered as an assisted injection to types
 * using Dagger [AssistedInject]. For these cases, the [AssistedFactory] -annotated interface should
 * be annotated with [CircuitPresenter] instead of the enclosing class.
 *
 * Types available for assisted injection are:
 * - [Screen] – the screen key used to create the [Presenter].
 * - [Navigator]
 * - [CircuitConfig]
 *
 * Each should only be defined at-most once.
 *
 * **Examples**
 * ```kotlin
 * // Function example
 * @CircuitPresenter(HomeScreen::class, AppScope::class)
 * @Composable
 * fun HomePresenter(screen: Screen, navigator: Navigator): HomeState { ... }
 *
 * // Class example
 * class HomePresenter @AssistedInject constructor(
 *   @Assisted screen: Screen,
 *   @Assisted navigator: Navigator,
 *   ...
 * ) : Presenter<HomeState> {
 *   // ...
 *
 *   @CircuitPresenter(HomeScreen::class, AppScope::class)
 *   @AssistedFactory
 *   fun interface Factory {
 *     fun create(screen: Screen, navigator: Navigator): HomePresenter
 *   }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class CircuitPresenter(
  val screen: KClass<out Screen>,
  val scope: KClass<*>,
)

/**
 * This annotation is used to mark a ui class or function for code generation. When annotated, the
 * corresponding factory will be generated and keyed with the defined [screen].
 *
 * The generated factories are then contributed to Anvil via [ContributesMultibinding] and scoped
 * with the provided [scope] key.
 *
 * ## Classes
 *
 * [Ui] classes can be annotated and have their corresponding [Ui.Factory] class generated for them.
 *
 * **UI**
 * ```kotlin
 * @CircuitUi(HomeScreen::class, AppScope::class)
 * class HomeUi @Inject constructor(...) : Ui<HomeState> { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomeUiFactory @Inject constructor() : Ui.Factory { ... }
 * ```
 *
 * ## Functions
 *
 * Simple functions can be annotated and have a corresponding [Ui.Factory] generated. This is
 * primarily useful for simple cases where a class is just technical tedium.
 *
 * **Requirements**
 * - UI functions can optionally accept a [CircuitUiState] type as a parameter, but it is not
 * required.
 * - UI functions _must_ return [Unit].
 * - UI functions _must_ be [Composable].
 *
 * **UI**
 * ```kotlin
 * @CircuitUi(HomeScreen::class, AppScope::class)
 * @Composable
 * fun Home(state: HomeState) { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomeUiFactory @Inject constructor() : Ui.Factory { ... }
 * ```
 *
 * ## Assisted injection
 *
 * Any type that is offered in [Ui.Factory] can be offered as an assisted injection to types using
 * Dagger [AssistedInject]. For these cases, the [AssistedFactory] -annotated interface should be
 * annotated with [CircuitUi] instead of the enclosing class.
 *
 * Types available for assisted injection are:
 * - [Screen] – the screen key used to create the [Ui].
 * - [CircuitConfig]
 *
 * Each should only be defined at-most once.
 *
 * **Examples**
 * ```kotlin
 * // Function example
 * @CircuitUi(HomeScreen::class, AppScope::class)
 * @Composable
 * fun HomeUi(state: HomeState) { ... }
 *
 * // Class example
 * class HomeUi @AssistedInject constructor(
 *   @Assisted screen: Screen,
 *   ...
 * ) : Ui<HomeState> {
 *   // ...
 *
 *   @CircuitUi(HomeScreen::class, AppScope::class)
 *   @AssistedFactory
 *   fun interface Factory {
 *     fun create(state: HomeState): : ScreenUi?
 *   }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class CircuitUi(
  val screen: KClass<out Screen>,
  val scope: KClass<*>,
)

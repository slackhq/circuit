// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.codegen.annotations

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation

/**
 * This annotation is used to mark a UI or presenter class or function for code generation. When
 * annotated, the type's corresponding factory will be generated and keyed with the defined [screen]
 * .
 *
 * The generated factories are then contributed to Anvil via
 * [com.squareup.anvil.annotations.ContributesMultibinding] and scoped with the provided [scope]
 * key.
 *
 * ## Classes
 *
 * [Presenter] and [Ui] classes can be annotated and have their corresponding [Presenter.Factory] or
 * [Ui.Factory] classes generated for them.
 *
 * **Presenter**
 *
 * ```kotlin
 * @CircuitInject(HomeScreen::class, AppScope::class)
 * class HomePresenter @Inject constructor(...) : Presenter<HomeState> { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomePresenterFactory @Inject constructor() : Presenter.Factory { ... }
 * ```
 *
 * **UI**
 *
 * ```kotlin
 * @CircuitInject(HomeScreen::class, AppScope::class)
 * class HomeUi @Inject constructor(...) : Ui<HomeState> { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomeUiFactory @Inject constructor() : Ui.Factory { ... }
 * ```
 *
 * ## Functions
 *
 * Simple functions can be annotated and have a corresponding [Presenter.Factory] generated. This is
 * primarily useful for simple cases where a class is just technical tedium.
 *
 * **Requirements**
 * - Presenter function names _must_ end in `Presenter`, otherwise they will be treated as UI
 *   functions.
 * - Presenter functions _must_ return a [CircuitUiState] type.
 * - UI functions can optionally accept a [CircuitUiState] type as a parameter, but it is not
 *   required.
 * - UI functions _must_ return [Unit].
 * - Both presenter and UI functions _must_ be [Composable].
 *
 * **Presenter**
 *
 * ```kotlin
 * @CircuitInject(HomeScreen::class, AppScope::class)
 * @Composable
 * fun HomePresenter(): HomeState { ... }
 *
 * // Generates
 * @ContributesMultibinding(AppScope::class)
 * class HomePresenterFactory @Inject constructor() : Presenter.Factory { ... }
 * ```
 *
 * **UI**
 *
 * ```kotlin
 * @CircuitInject(HomeScreen::class, AppScope::class)
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
 * Any type that is offered in [Presenter.Factory] and [Ui.Factory] can be offered as an assisted
 * injection to types using Dagger [dagger.assisted.AssistedInject]. For these cases, the
 * [dagger.assisted.AssistedFactory] -annotated interface should be annotated with [CircuitInject]
 * instead of the enclosing class.
 *
 * Types available for assisted injection are:
 * - [Screen] – the screen key used to create the [Presenter] or [Ui].
 * - [Navigator] – (presenters only)
 * - [Circuit]
 *
 * Each should only be defined at-most once.
 *
 * **Examples**
 *
 * ```kotlin
 * // Function example
 * @CircuitInject(HomeScreen::class, AppScope::class)
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
 *   @CircuitInject(HomeScreen::class, AppScope::class)
 *   @AssistedFactory
 *   fun interface Factory {
 *     fun create(screen: Screen, navigator: Navigator): HomePresenter
 *   }
 * }
 * ```
 */
@ContributingAnnotation
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public expect annotation class CircuitInject(val screen: KClass<out Screen>, val scope: KClass<*>)

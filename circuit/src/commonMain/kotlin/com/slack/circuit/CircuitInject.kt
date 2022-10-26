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

import kotlin.reflect.KClass

/**
 * This annotation is used to mark a UI or presenter class or function for code generation. When
 * annotated, the type's corresponding factory will be generated and key'd with the defined [screen]
 * .
 *
 * The generated factories are then contributed to Anvil via `@ContributesMultibinding` and scoped
 * with the provided [scope] key.
 *
 * ## Classes
 *
 * [Presenter] and [Ui] classes can be annotated and have their corresponding [Presenter.Factory] or
 * [Ui.Factory] classes generated for them.
 *
 * **Presenter**
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
 * functions.
 * - Presenter functions _must_ return a [CircuitUiState] type.
 * - UI functions can optionally accept a [CircuitUiState] type as a parameter, but it is not
 * required.
 * - UI functions _must_ return [Unit].
 * - Both presenter and UI functions _must_ be `@Composable`.
 *
 * **Presenter**
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
 * injection to types using Dagger `AssistedInject`. For these cases, the
 * `AssistedFactory`-annotated interface should be annotated with [CircuitInject] instead of the
 * enclosing class.
 *
 * Types available for assisted injection are:
 * - [Screen] – the screen key used to create the [Presenter] or [Ui].
 * - [Navigator] – (presenters only)
 * - [CircuitConfig]
 *
 * Each should only be defined at-most once.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class CircuitInject(
  val screen: KClass<out Screen>,
  val scope: KClass<*>,
)

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.subcircuit

import kotlin.reflect.KClass

/**
 * Marks a [SubPresenter] class or [SubUi] function for code generation.
 *
 * SubCircuit is a lightweight alternative to Circuit for rendering nested presenter/UI pairs that
 * delegate events to an outer component rather than handling navigation themselves.
 *
 * When applied to a presenter class with an `@AssistedFactory` nested interface, generates a
 * [SubPresenterFactory] that is contributed to the DI graph via multibinding.
 *
 * When applied to a `@Composable` UI function, generates a [SubUiFactory] that is contributed to
 * the DI graph via multibinding.
 *
 * ## Usage on Presenter Classes
 *
 * The presenter must implement [SubPresenter] with an event type extending [SubCircuitOuterEvent]
 * and a state type extending [SubCircuitUiState]:
 * ```kotlin
 * @SubCircuitInject(MyScreen::class, AppScope::class)
 * class MyPresenter @AssistedInject constructor(
 *   @Assisted val screen: MyScreen
 * ) : SubPresenter<MyOuterEvent, MyState> {
 *
 *   @Composable
 *   override fun present(outerEventSink: (MyOuterEvent) -> Unit): MyState {
 *     return MyState(screen, outerEventSink)
 *   }
 *
 *   @AssistedFactory
 *   interface Factory {
 *     fun create(screen: MyScreen): MyPresenter
 *   }
 * }
 * ```
 *
 * ## Usage on UI Functions
 *
 * The UI function's state parameter must extend [SubCircuitUiState]:
 * ```kotlin
 * @SubCircuitInject(MyScreen::class, AppScope::class)
 * @Composable
 * fun MyUi(state: MyState, modifier: Modifier = Modifier) {
 *   // UI implementation
 * }
 * ```
 *
 * @property screen The [SubScreen] class that this presenter or UI handles.
 * @property scope The DI scope to contribute the generated factory to.
 * @see SubPresenter
 * @see SubUi
 * @see SubScreen
 * @see SubCircuitOuterEvent
 * @see SubCircuitUiState
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class SubCircuitInject(val screen: KClass<*>, val scope: KClass<*>)

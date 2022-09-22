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
 * Marker interface for all UiState types.
 *
 * States in Circuit should be minimal data models that [UIs][Ui] can render. They are produced by
 * [presenters][Presenter] that interpret the underlying data layer and mediate input user/nav
 * [events][CircuitUiEvent].
 *
 * [UIs][Ui] take states as parameters and should act as pure functions that render the input state
 * as a UI. They should not have any side effects or directly interact with the underlying data
 * layer.
 *
 * **Circuit state types should be truly immutable types.**
 */
@Immutable interface CircuitUiState

/**
 * Marker interface for all UiEvent types.
 *
 * Events in Circuit should generally reflect user interactions with the UI. They are mediated by
 * [presenters][Presenter] and may or may not influence the current [state][CircuitUiState].
 *
 * **Circuit event types should be truly immutable types.**
 */
@Immutable interface CircuitUiEvent


/**
 * Marker interface for composite (in other words nested) UiEvent types.
 *
 * Events in Circuit should generally reflect user interactions with the UI. They are mediated by
 * [presenters][Presenter] and may or may not influence the current [state][CircuitUiState].
 *
 * **Circuit event types should be truly immutable types.**
 */
@Immutable abstract class CompositeCircuitUiEvent(open val event: CircuitUiEvent) : CircuitUiEvent

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
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
@Immutable public interface CircuitUiState

/**
 * Marker interface for all UiEvent types.
 *
 * Events in Circuit should generally reflect user interactions with the UI. They are mediated by
 * [presenters][Presenter] and may or may not influence the current [state][CircuitUiState].
 *
 * **Circuit event types should be truly immutable types.**
 */
@Immutable public interface CircuitUiEvent

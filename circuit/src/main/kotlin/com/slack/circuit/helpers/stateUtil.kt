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
package com.slack.circuit.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import com.slack.circuit.CircuitUiEvent
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Presenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * Helper to [remember][remember] [NestedUiState] given [events] [Flow] of [CompositeEvent]s. Events
 * are filtered by instance of [NestedEventHolder] then mapped from the parent type [NestedUiEvent]
 * to the target [NavEventHolder] subtype, then we fetch and return the [NestedUiState] with the
 * given [presenter].
 *
 * Currently [mapper] has a bug where we get JVM IR: "IndexOutOfBoundsException: Cannot pop operand
 * off an empty stack" with crossinline lambdas and interface delegation. Being tracked here:
 * https://youtrack.jetbrains.com/issue/KT-51950
 *
 * @param presenter The [Presenter] used to take in a [Flow] of [NestedUiEvent]s then fetch and
 * return the [NestedUiState].
 * @param events The [Flow] of events to be filtered.
 * @param mapper Maps from the parent type [NestedUiEvent] to the target [NavEventHolder] subtype.
 */
@Composable
inline fun <
  CompositeEvent, reified NestedEventHolder, NestedUiState, NestedUiEvent> rememberNestedState(
  presenter: Presenter<NestedUiState, NestedUiEvent>,
  events: Flow<CompositeEvent>,
  crossinline mapper: @DisallowComposableCalls (NestedEventHolder) -> NestedUiEvent
): NestedUiState where
CompositeEvent : CircuitUiEvent,
NestedEventHolder : CompositeEvent,
NestedUiState : CircuitUiState,
NestedUiEvent : CircuitUiEvent {
  val rememberEventFlow = remember {
    events.filterIsInstance<NestedEventHolder>().map { mapper(it) }
  }
  return presenter.present(rememberEventFlow)
}

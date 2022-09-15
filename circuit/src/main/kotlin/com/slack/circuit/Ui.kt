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
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/**
 * Represents a composable UI for the given [UiState] and [UiEvent]. Conventionally, this should
 * just be the return type of a ui function and a thin shim over a "real" render implementation.
 *
 * This has two main benefits:
 * 1. Discouraging properties and general non-composable state that writing a class may invite.
 * 2. Ensuring separation of [Ui] instance from [Screen] specific ui composables allows for and
 * encourages easy UI previews via Compose's [@Preview][Preview] annotations.
 *
 * Usage:
 * ```
 * internal fun tacoUi(): Ui<State, Event> = ui { state, eventSink ->
 *   Tacos(state, eventSink)
 * }
 *
 * @Composable private fun Tacos(state: State, eventSink: (Event) -> Unit = {}) {...}
 *
 * @Preview
 * @Composable
 * private fun PreviewTacos() = Tacos(...)
 * ```
 *
 * This could be a class, but isn't necessary unless you're using dependency injection. Most UIs
 * don't use dependency injection at all however, unless maybe getting assisted injections of things
 * like image loaders.
 *
 * If a given [Ui] never emits events, then you can use [Nothing] for the [UiEvent] type instead.
 *
 * Note that due to a bug in studio, we can't make this a `fun interface` _yet_. Instead, use [ui].
 *
 * @see ui
 */
interface Ui<UiState : Any, UiEvent : Any> {
  @Composable fun Render(state: UiState, eventSink: (UiEvent) -> Unit)
}

/**
 * Due to this bug in Studio, we can't write lambda impls of [Ui] directly. This works around it by
 * offering a shim function of the same name. Once it's fixed, we can remove this and make [Ui] a
 * fun interface instead.
 *
 * Bug: https://issuetracker.google.com/issues/240292828
 *
 * @see [Ui] for main docs.
 */
inline fun <UiState : Any, UiEvent : Any> ui(
  crossinline body: @Composable (state: UiState, eventSink: (UiEvent) -> Unit) -> Unit
): Ui<UiState, UiEvent> {
  return object : Ui<UiState, UiEvent> {
    @Composable
    override fun Render(state: UiState, eventSink: (UiEvent) -> Unit) {
      body(state, eventSink)
    }
  }
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <E : Any> EventCollector(events: Flow<E>, noinline eventCollector: (event: E) -> Unit) {
  LaunchedEffect(events) { events.collect(eventCollector) }
}

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

/**
 * Represents a composable UI for the given [UiState]. Conventionally, this should just be the
 * return type of a ui function and a thin shim over a "real" render implementation.
 *
 * This has two main benefits:
 * 1. Discouraging properties and general non-composable state that writing a class may invite.
 * 2. Ensuring separation of [Ui] instance from [Screen] specific ui composables allows for and
 * encourages easy UI previews via Compose's [@Preview][Preview] annotations.
 *
 * Usage:
 * ```
 * internal fun tacoUi(): Ui<State> = ui { state ->
 *   Tacos(state)
 * }
 *
 * @Composable private fun Tacos(state: State) {...}
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
 * If a given [Presenter] only ever emits the same state, you can define a single value-less
 * `object` type for the state.
 *
 * Note that due to a bug in studio, we can't make this a `fun interface` _yet_. Instead, use [ui].
 *
 * @see ui
 */
interface Ui<UiState : CircuitUiState> {
  @Composable fun Render(state: UiState)

  /**
   * A factory that creates [ScreenUis][ScreenUi], which in turn contain the desired [Ui] for a
   * given [Screen].
   *
   * Note that individual UIs should just be top-level [ui] function calls that factories simply
   * call into. This allows easily standing up composable preview functions.
   *
   * ```
   * class FavoritesUiFactory @Inject constructor() : Ui.Factory {
   *  override fun create(
   *    screen: Screen,
   *  ): ScreenUi? {
   *    val ui = when (screen) {
   *      is AddFavorites -> {
   *        addFavoritesUi()
   *      }
   *      else -> return null
   *    }
   *    return ScreenUi(
   *      ui = ui as Ui<*, *>,
   *    )
   *   }
   * }
   *
   * private fun addFavoritesUi() =
   *   ui<AddFavorites.State> { state -> Favorites(state) }
   *
   * @Composable private fun Favorites(state: State) {...}
   * ```
   */
  fun interface Factory {
    fun create(screen: Screen): ScreenUi?
  }
}

data class ScreenUi(val ui: Ui<*>)

/**
 * Due to this bug in Studio, we can't write lambda impls of [Ui] directly. This works around it by
 * offering a shim function of the same name. Once it's fixed, we can remove this and make [Ui] a
 * fun interface instead.
 *
 * Bug: https://issuetracker.google.com/issues/240292828
 *
 * @see [Ui] for main docs.
 */
inline fun <UiState : CircuitUiState> ui(
  crossinline body: @Composable (state: UiState) -> Unit
): Ui<UiState> {
  return object : Ui<UiState> {
    @Composable
    override fun Render(state: UiState) {
      body(state)
    }
  }
}

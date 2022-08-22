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

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Presents a given [UiState] and handles its [events][UiEvent].
 *
 * TODO doc factory patterns TODO doc accepting Screens and Navigator params
 */
interface Presenter<UiState, UiEvent : Any> where UiState : Any, UiState : Parcelable {
  /**
   * // TODO out of date - update!!
   * The primary entry point to present a [Composable] [render] to connect this [Presenter] with a
   * given [Ui], usually automatically handled by a [Navigator]. The structure of this function is
   * based around two parameters:
   * 1. `state` - the current `UiState`.
   * 2. `uiEvents` - a callback to listen to UiEvent emissions from the corresponding [Ui].
   *
   * Then when implementing, the [render] function is the jumping off point to the [Ui]. Usually,
   * state is modeled as a [mutableStateOf] and then passed in to the [render] function. Then any
   * time the state updates, the [render] function is recomposed accordingly.
   *
   * ```kotlin
   * class FavoritesPresenter : Presenter<State, Event> {
   *   @Composable override fun present(render: Renderer<State, Event>) {
   *     var state by rememberSaveable { mutableStateOf(initialState) }
   *     // ...
   *     render(state) { event ->
   *       // Handle UI events here
   *     }
   *   }
   * }
   * ```
   */
  @Composable fun present(events: Flow<UiEvent>): UiState
}

fun interface PresenterFactory {
  fun create(screen: Screen, navigator: Navigator): Presenter<*, *>?
}

// Example
// class FavoritesPresenterFactory @Inject constructor(
//  private val addFavoritesPresenter: AddFavoritesPresenter.Factory,
// ) : PresenterFactory {
//
//  override fun create(
//    screen: Screen,
//    navigator: Navigator
//  ): Presenter<*, *>? {
//    return when (screen) {
//      is AddFavorites -> addFavoritesPresenter.create(
//        navigator = navigator,
//        args = screen,
//      ).asPresenter()
//      else -> null
//    }
//  }
// }

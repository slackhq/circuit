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

/**
 * A factory that creates [ScreenViews][ScreenView], which in turn contain the desired [Ui] for a
 * given [Screen].
 *
 * Note that individual UIs should just be top-level [ui] function calls that factories simply call
 * into. This allows easily standing up composable preview functions.
 *
 * ```
 * class FavoritesViewFactory @Inject constructor() : ViewFactory {
 *  override fun createView(
 *    screen: Screen,
 *  ): ScreenView? {
 *    val view = when (screen) {
 *      is AddFavorites -> {
 *        addFavoritesUi()
 *      }
 *      else -> return null
 *    }
 *    return ScreenView(
 *      ui = view as Ui<*, *>,
 *    )
 *   }
 * }
 *
 * private fun addFavoritesUi() =
 *   ui<AddFavorites.State, AddFavorites.Event> { state, events -> Favorites(state, events) }
 *
 * @Composable private fun Favorites(state: State, ui: (Event) -> Unit = {}) {...}
 * ```
 */
fun interface ScreenViewFactory {
  fun createView(screen: Screen): ScreenView?
}

data class ScreenView(
  val ui: Ui<*, *>,
// TODO does this kind of thing eventually move to compose Modifier instead?
//  val uiMetadata: UiMetadata = UiMetadata()
)

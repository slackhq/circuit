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
 * A factory that creates [ScreenUis][ScreenUi], which in turn contain the desired [Ui] for a given
 * [Screen].
 *
 * Note that individual UIs should just be top-level [ui] function calls that factories simply call
 * into. This allows easily standing up composable preview functions.
 *
 * ```
 * class FavoritesUiFactory @Inject constructor() : UiFactory {
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
 *   ui<AddFavorites.State, AddFavorites.Event> { state, eventSink -> Favorites(state, eventSink) }
 *
 * @Composable private fun Favorites(state: State, eventSink: (Event) -> Unit = {}) {...}
 * ```
 */
fun interface UiFactory {
  fun create(screen: Screen): ScreenUi?
}

data class ScreenUi(
  val ui: Ui<*, *>,
// TODO does this kind of thing eventually move to compose Modifier instead?
//  val uiMetadata: UiMetadata = UiMetadata()
)

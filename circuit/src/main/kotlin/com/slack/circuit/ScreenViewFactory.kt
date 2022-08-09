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

/** TODO */
fun interface ScreenViewFactory {
  fun createView(screen: Screen, container: ContentContainer): ScreenView?
}

data class ScreenView(
  val container: ContentContainer,
  val ui: Ui<*, *>,
// TODO does this kind of thing eventually move to compose Modifier instead?
//  val uiMetadata: UiMetadata = UiMetadata()
)

// Example
// class FavoritesViewFactory @Inject constructor(
//  private val picasso: Picasso,
// ) : ViewFactory {
//  override fun createView(
//    screen: Screen,
//    context: Context,
//    parent: ViewGroup
//  ): ScreenView? {
//    val view = when (screen) {
//      is AddFavorites -> {
//        AddFavoritesView(
//          context = context,
//          picasso = picasso,
//        )
//      }
//      else -> return null
//    }
//
//    return ScreenView(
//      view = view,
//      ui = view as Ui<*, *>,
//      uiMetadata = UiMetadata(treatment = FULL_SCREEN, hideTabs = true)
//    )
//  }
// }

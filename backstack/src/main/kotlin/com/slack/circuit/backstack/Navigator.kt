/*
 * Copyright (C) 2022 Adam Powell
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
package com.slack.circuit.backstack

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Presentation logic for currently visible routes of a navigable UI. */
@Stable
interface NavigatorRouteDecoration {
  @Composable
  fun <T> DecoratedContent(
    arg: T,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit
  )
}

/** Default values and common alternatives used by the [Navigator] family of composables. */
object NavigatorDefaults {

  /** The default [NavigatorRouteDecoration] used by [Navigator]. */
  object DefaultDecoration : NavigatorRouteDecoration {
    @Composable
    override fun <T> DecoratedContent(
      arg: T,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit
    ) {
      Crossfade(arg, modifier, content = content)
    }
  }

  /**
   * An empty [NavigatorRouteDecoration] that emits the content with no surrounding decoration or
   * logic.
   */
  object EmptyDecoration : NavigatorRouteDecoration {
    @Composable
    override fun <T> DecoratedContent(
      arg: T,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit
    ) {
      content(arg)
    }
  }

  /**
   * Bright ugly error text telling a developer they didn't provide a route that a [BackStack] asked
   * for.
   */
  val UnavailableRoute: @Composable (String) -> Unit = { route ->
    BasicText(
      "Route not available: $route",
      Modifier.background(Color.Red),
      style = TextStyle(color = Color.Yellow)
    )
  }
}

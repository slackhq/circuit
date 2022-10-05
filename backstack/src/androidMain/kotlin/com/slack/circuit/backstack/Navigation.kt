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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Presentation logic for currently visible routes of a navigable UI. */
@Stable
public interface NavDecoration {
  @Composable
  public fun <T> DecoratedContent(
    arg: T,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit
  )
}

/** Default values and common alternatives used by navigable composables. */
public object NavigatorDefaults {

  private const val FIVE_PERCENT = 0.05f
  private val SlightlyRight = { width: Int -> (width * FIVE_PERCENT).toInt() }
  private val SlightlyLeft = { width: Int -> 0 - (width * FIVE_PERCENT).toInt() }

  /** The default [NavDecoration] used in navigation. */
  public object DefaultDecoration : NavDecoration {
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun <T> DecoratedContent(
      arg: T,
      backStackDepth: Int,
      modifier: Modifier,
      content: @Composable (T) -> Unit
    ) {
      // Remember the previous stack depth so we know if the navigation is going "back".
      val prevStackDepth = rememberSaveable { mutableStateOf(backStackDepth) }
      val diff = backStackDepth - prevStackDepth.value
      prevStackDepth.value = backStackDepth
      AnimatedContent(
        targetState = arg,
        modifier = modifier,
        transitionSpec = {
          // Mirror the forward and backward transitions of activities in Android 33
          if (diff > 0) {
            slideInHorizontally(tween(), SlightlyRight) + fadeIn() with
              slideOutHorizontally(tween(), SlightlyLeft) + fadeOut()
          } else
            if (diff < 0) {
                slideInHorizontally(tween(), SlightlyLeft) + fadeIn() with
                  slideOutHorizontally(tween(), SlightlyRight) + fadeOut()
              } else {
                // Crossfade if there was no diff
                fadeIn() with fadeOut()
              }
              .using(
                // Disable clipping since the faded slide-in/out should
                // be displayed out of bounds.
                SizeTransform(clip = false)
              )
        },
      ) {
        content(it)
      }
    }
  }

  /** An empty [NavDecoration] that emits the content with no surrounding decoration or logic. */
  public object EmptyDecoration : NavDecoration {
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
  public val UnavailableRoute: @Composable (String) -> Unit = { route ->
    BasicText(
      "Route not available: $route",
      Modifier.background(Color.Red),
      style = TextStyle(color = Color.Yellow)
    )
  }
}

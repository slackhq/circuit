// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.navigation.NavArgument

@Stable
@ExperimentalNavStageApi
public fun interface PaneTransition {
  @Composable
  public fun <T : NavArgument> AnimatedPaneContent(
    targetItem: T,
    paneKey: Any,
    navEvent: PaneNavEvent,
    content: @Composable (T) -> Unit,
  )

  public companion object {
    public val Default: PaneTransition = PaneTransition { targetItem, _, navEvent, content ->
      val isForward = navEvent == PaneNavEvent.GoTo || navEvent == PaneNavEvent.Forward
      AnimatedContent(
        targetState = targetItem,
        contentKey = { it.key },
        transitionSpec = {
          if (isForward) {
            (slideInHorizontally(tween()) { it / 4 } + fadeIn(tween()))
              .togetherWith(slideOutHorizontally(tween()) { -it / 4 } + fadeOut(tween()))
          } else {
            (slideInHorizontally(tween()) { -it / 4 } + fadeIn(tween()))
              .togetherWith(slideOutHorizontally(tween()) { it / 4 } + fadeOut(tween()))
          }
        },
      ) { item ->
        content(item)
      }
    }

    public val None: PaneTransition = PaneTransition { targetItem, _, _, content ->
      content(targetItem)
    }

    public val Crossfade: PaneTransition = PaneTransition { targetItem, _, _, content ->
      androidx.compose.animation.Crossfade(
        targetState = targetItem,
        contentKey = { it.key },
      ) { item ->
        content(item)
      }
    }
  }
}

@ExperimentalNavStageApi
public enum class PaneNavEvent {
  GoTo,
  Pop,
  RootReset,
  Forward,
  Backward,
}

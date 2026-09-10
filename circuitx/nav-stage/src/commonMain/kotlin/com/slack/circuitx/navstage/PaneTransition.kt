// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.runtime.navigation.NavArgument

/**
 * Controls the animation when the content within a single pane changes.
 *
 * Each pane in a [NavStage] can have its own [PaneTransition]. The [navEvent] is provided so
 * transitions can animate directionally (e.g. slide left on forward, slide right on backward).
 */
@Stable
@ExperimentalNavStageApi
public interface PaneTransition {
  @Composable
  public fun <T : NavArgument> AnimatedPaneContent(
    targetItem: T,
    paneKey: Any,
    navEvent: AnimatedNavEvent,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
  )

  public companion object {
    public val Default: PaneTransition =
      object : PaneTransition {
        @Composable
        override fun <T : NavArgument> AnimatedPaneContent(
          targetItem: T,
          paneKey: Any,
          navEvent: AnimatedNavEvent,
          modifier: Modifier,
          content: @Composable (T) -> Unit,
        ) {
          val isForward =
            navEvent == AnimatedNavEvent.GoTo || navEvent == AnimatedNavEvent.Forward
          AnimatedContent(
            targetState = targetItem,
            contentKey = { it.key },
            modifier = modifier,
            transitionSpec = {
              if (isForward) {
                (slideInHorizontally(tween()) { it / 4 } + fadeIn(tween())).togetherWith(
                  slideOutHorizontally(tween()) { -it / 4 } + fadeOut(tween())
                )
              } else {
                (slideInHorizontally(tween()) { -it / 4 } + fadeIn(tween())).togetherWith(
                  slideOutHorizontally(tween()) { it / 4 } + fadeOut(tween())
                )
              }
            },
          ) { item ->
            content(item)
          }
        }
      }

    public val None: PaneTransition =
      object : PaneTransition {
        @Composable
        override fun <T : NavArgument> AnimatedPaneContent(
          targetItem: T,
          paneKey: Any,
          navEvent: AnimatedNavEvent,
          modifier: Modifier,
          content: @Composable (T) -> Unit,
        ) {
          Box(modifier) { content(targetItem) }
        }
      }

    public val Crossfade: PaneTransition =
      object : PaneTransition {
        @Composable
        override fun <T : NavArgument> AnimatedPaneContent(
          targetItem: T,
          paneKey: Any,
          navEvent: AnimatedNavEvent,
          modifier: Modifier,
          content: @Composable (T) -> Unit,
        ) {
          // Each slot renders its own item. Rendering the incoming item in both would compose the
          // same record twice and defeat the crossfade.
          AnimatedContent(
            targetState = targetItem,
            contentKey = { it.key },
            modifier = modifier,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
          ) { item ->
            content(item)
          }
        }
      }
  }
}

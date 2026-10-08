// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay

/**
 * A custom [SharedElementTransitionScope.AnimatedScope] for shared element transitions within or
 * between panes.
 */
@ExperimentalNavStageApi public object Pane : SharedElementTransitionScope.AnimatedScope

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
        @OptIn(ExperimentalSharedTransitionApi::class)
        @Composable
        override fun <T : NavArgument> AnimatedPaneContent(
          targetItem: T,
          paneKey: Any,
          navEvent: AnimatedNavEvent,
          modifier: Modifier,
          content: @Composable (T) -> Unit,
        ) {
          AnimatedContent(
            targetState = targetItem,
            contentKey = { it.key },
            modifier = modifier,
            transitionSpec = {
              when (navEvent) {
                AnimatedNavEvent.GoTo,
                AnimatedNavEvent.Forward ->
                  (slideInHorizontally(tween()) { it / 4 } + fadeIn(tween())).togetherWith(
                    slideOutHorizontally(tween()) { -it / 4 } + fadeOut(tween())
                  )
                AnimatedNavEvent.Pop,
                AnimatedNavEvent.Backward ->
                  (slideInHorizontally(tween()) { -it / 4 } + fadeIn(tween())).togetherWith(
                    slideOutHorizontally(tween()) { it / 4 } + fadeOut(tween())
                  )
                AnimatedNavEvent.RootReset -> fadeIn(tween()).togetherWith(fadeOut(tween()))
              }
            },
          ) { item ->
            ProvideAnimatedTransitionScope(Pane, this@AnimatedContent) { content(item) }
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

/**
 * Resolves whichever of the pane, stage ([Navigation]), or overlay scopes is currently
 * transitioning, so shared elements follow both in-pane navigation and stage changes. Falls back to
 * the first available in that order when none is.
 */
@ExperimentalNavStageApi
public fun SharedElementTransitionScope.findActiveStageScope(): AnimatedVisibilityScope? {
  val scopes =
    listOfNotNull(
      findAnimatedScope(Pane),
      findAnimatedScope(Navigation),
      findAnimatedScope(Overlay),
    )
  return scopes.firstOrNull { it.transition.currentState != it.transition.targetState }
    ?: scopes.firstOrNull()
}

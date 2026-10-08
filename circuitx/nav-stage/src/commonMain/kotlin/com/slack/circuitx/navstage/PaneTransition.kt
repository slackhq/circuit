// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay

/**
 * A custom [SharedElementTransitionScope.AnimatedScope] for shared element transitions within or
 * between panes.
 */
@ExperimentalNavStageApi
public object PaneAnimatedScope : SharedElementTransitionScope.AnimatedScope

/**
 * Controls the animation when the content within a single pane changes.
 *
 * Each pane in a [NavStage] can have its own [PaneTransition]. The [navEvent] is provided so
 * transitions can animate directionally (e.g. slide left on forward, slide right on backward).
 */
@Stable
@ExperimentalNavStageApi
public interface PaneTransition {
  /**
   * Renders [targetItem], optionally animating from the items it replaces.
   *
   * Call [content] once per item, from the [AnimatedVisibilityScope] that animates that item, like
   * `AnimatedContent`'s content scope. The pane uses it to provide [PaneAnimatedScope] and to tell
   * the item entering from those leaving, so a record moving between panes keeps its state.
   */
  @Composable
  public fun <T : NavArgument> AnimatedPaneContent(
    targetItem: T,
    paneKey: Any,
    navEvent: AnimatedNavEvent,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit,
  )

  public companion object {
    public val Default: PaneTransition =
      object : PaneTransition {
        @OptIn(InternalCircuitApi::class)
        @Composable
        override fun <T : NavArgument> AnimatedPaneContent(
          targetItem: T,
          paneKey: Any,
          navEvent: AnimatedNavEvent,
          modifier: Modifier,
          content: @Composable AnimatedVisibilityScope.(T) -> Unit,
        ) {
          AnimatedContent(
            targetState = targetItem,
            contentKey = { it.key },
            modifier = modifier,
            label = "PaneTransition.Default $paneKey",
            transitionSpec = {
              when (navEvent) {
                AnimatedNavEvent.GoTo,
                AnimatedNavEvent.Forward -> NavigatorDefaults.forward
                AnimatedNavEvent.Pop,
                AnimatedNavEvent.Backward -> NavigatorDefaults.backward
                AnimatedNavEvent.RootReset -> fadeIn() togetherWith fadeOut()
              }.using(SizeTransform(clip = false))
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
          content: @Composable AnimatedVisibilityScope.(T) -> Unit,
        ) {
          key(targetItem.key) {
            AnimatedVisibility(
              visible = true,
              modifier = modifier,
              enter = EnterTransition.None,
              exit = ExitTransition.None,
              label = "PaneTransition.None $paneKey",
            ) {
              content(targetItem)
            }
          }
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
          content: @Composable AnimatedVisibilityScope.(T) -> Unit,
        ) {
          // Each slot renders its own item. Rendering the incoming item in both would compose the
          // same record twice and defeat the crossfade.
          AnimatedContent(
            targetState = targetItem,
            contentKey = { it.key },
            modifier = modifier,
            label = "PaneTransition.Crossfade $paneKey",
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
      findAnimatedScope(PaneAnimatedScope),
      findAnimatedScope(Navigation),
      findAnimatedScope(Overlay),
    )
  return scopes.firstOrNull { it.transition.currentState != it.transition.targetState }
    ?: scopes.firstOrNull()
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedTransitionKey

/**
 * Internal key type for shared element transitions between nav stage pane layouts. Keyed by the
 * navigation item's key so matching items in outgoing/incoming stages share the same bounds.
 */
internal data class NavStagePaneKey(val itemKey: Any) : SharedTransitionKey

@OptIn(ExperimentalNavStageApi::class, ExperimentalSharedTransitionApi::class)
internal class NavStagePaneScopeImpl<T : NavArgument>(
  private val content: @Composable (T) -> Unit,
  private val navEvent: PaneNavEvent,
  private val placeholderItemKeys: Set<Any> = emptySet(),
) : NavStagePaneScope<T> {

  @Composable
  override fun Pane(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    val isPlaceholder = item.key in placeholderItemKeys

    if (!SharedElementTransitionScope.isAvailable) {
      // No shared elements available — render without shared bounds.
      // Placeholder mode still prevents the movableContent crash.
      if (isPlaceholder) {
        Box(modifier)
      } else {
        RealPaneContent(key, item, modifier, transition)
      }
      return
    }

    SharedElementTransitionScope {
      val animatedScope = findAnimatedScope(Navigation)
      val paneModifier =
        if (animatedScope != null) {
          modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = NavStagePaneKey(item.key)),
            animatedVisibilityScope = animatedScope,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
          )
        } else {
          modifier
        }

      if (isPlaceholder) {
        // Empty box with shared bounds — the shared element system animates these bounds
        // to/from the matching pane in the incoming/outgoing stage.
        Box(paneModifier)
      } else {
        RealPaneContent(key, item, paneModifier, transition)
      }
    }
  }

  @Composable
  private fun RealPaneContent(key: Any, item: T, modifier: Modifier, transition: PaneTransition) {
    transition.AnimatedPaneContent(
      targetItem = item,
      paneKey = key,
      navEvent = navEvent,
      modifier = modifier,
    ) { targetItem ->
      Box(Modifier) { content(targetItem) }
    }
  }
}

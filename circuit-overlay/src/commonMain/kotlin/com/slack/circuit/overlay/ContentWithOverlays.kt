// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Renders the given [content] with the ability to show overlays on top of it. This works by
 * exposing an [OverlayHost] via [LocalOverlayHost].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param overlayHost the [OverlayHost] to use for managing overlays.
 * @param content The regular content to render. Any overlays will be rendered over them.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
public fun ContentWithOverlays(
  modifier: Modifier = Modifier,
  overlayHost: OverlayHost = rememberOverlayHost(),
  content: @Composable () -> Unit
) {
  CompositionLocalProvider(LocalOverlayHost provides overlayHost) {
    Box(modifier) {
      content()
      val transition = updateTransition(overlayHost.currentOverlayData, label = null)
      val enterTransition =
        remember(transition.targetState) {
          (transition.targetState?.overlay as? AnimatedOverlay)?.enterTransition
            ?: EnterTransition.None
        }
      val exitTransition =
        remember(transition.currentState) {
          (transition.currentState?.overlay as? AnimatedOverlay)?.exitTransition
            ?: ExitTransition.None
        }
      transition.AnimatedVisibility(
        visible = { it != null },
        modifier = Modifier.fillMaxSize(),
        enter = enterTransition,
        exit = exitTransition,
      ) {
        // Compute the current overlay data from the transition.
        // If transitioning from null to not null, we want to use the targetState. And vice-versa,
        // if transitioning from not null to null, we want to use the currentState.
        val overlayData by remember {
          derivedStateOf { transition.currentState ?: transition.targetState }
        }
        overlayData?.let { data ->
          val animatedOverlay = data.overlay as? AnimatedOverlay
          if (animatedOverlay != null) {
            with(animatedOverlay) { AnimatedContent(data::finish) }
          } else {
            data.overlay.Content(data::finish)
          }
        }
      }
    }
  }
}

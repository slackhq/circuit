// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Renders the given [content] with the ability to show overlays on top of it. This works by
 * exposing an [OverlayHost] via [LocalOverlayHost].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param overlayHost the [OverlayHost] to use for managing overlays.
 * @param content The regular content to render. Any overlays will be rendered over them.
 */
@Composable
public fun ContentWithOverlays(
  modifier: Modifier = Modifier,
  overlayHost: OverlayHost = rememberOverlayHost(),
  content: @Composable () -> Unit
) {
  val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
  val overlayState by remember {
    derivedStateOf {
      overlayHostData?.let { OverlayState.SHOWING } ?: OverlayState.HIDDEN
    }
  }
  CompositionLocalProvider(LocalOverlayHost provides overlayHost, LocalOverlayState provides overlayState) {
    Box(modifier) {
      content()
      AnimatedContent(
        targetState = overlayHostData,
        transitionSpec = {
          val enter =
            (targetState?.overlay as? AnimatedOverlay)?.enterTransition ?: EnterTransition.None
          val exit =
            (initialState?.overlay as? AnimatedOverlay)?.exitTransition ?: ExitTransition.None
          val sizeTransform = if (targetState != null) SizeTransform { _, _ -> snap(0) } else null
          (enter togetherWith exit).using(sizeTransform).also {
            it.targetContentZIndex = targetState?.let { 1f } ?: -1f
          }
        },
        contentAlignment = Alignment.Center
      ) { data ->
        when (val overlay = data?.overlay) {
          null -> Unit
          is AnimatedOverlay -> with(overlay) { AnimatedContent(data::finish) }
          else -> overlay.Content(data::finish)
        }
      }
    }
  }
}

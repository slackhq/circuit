// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.EnterExitState.PreEnter
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.ProvideAnimatedTransitionScope
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Overlay

/**
 * Renders the given [content] with the ability to show overlays on top of it. This works by
 * exposing an [OverlayHost] via [LocalOverlayHost].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param overlayHost the [OverlayHost] to use for managing overlays.
 * @param content The regular content to render. Any overlays will be rendered over them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun ContentWithOverlays(
  modifier: Modifier = Modifier,
  overlayHost: OverlayHost = rememberOverlayHost(),
  content: @Composable () -> Unit,
) {
  val overlayHostData by rememberUpdatedState(overlayHost.currentOverlayData)
  val overlayState by remember {
    derivedStateOf { overlayHostData?.let { OverlayState.SHOWING } ?: OverlayState.HIDDEN }
  }
  CompositionLocalProvider(
    LocalOverlayHost provides overlayHost,
    LocalOverlayState provides overlayState,
  ) {
    Box(modifier) {
      val transition =
        updateTransition(targetState = overlayHostData, label = "OverlayHostData transition")
      ProvideAnimatedTransitionScope(Overlay, transition.animatedVisibilityScope { it == null }) {
        content()
      }
      transition.AnimatedContent(
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
        contentAlignment = Alignment.Center,
      ) { data ->
        ProvideAnimatedTransitionScope(Overlay, this) {
          when (val overlay = data?.overlay) {
            null -> Unit
            is AnimatedOverlay -> with(overlay) { AnimatedContent(data::finish) }
            else -> overlay.Content(data::finish)
          }
        }
      }
    }
  }
}

/** Creates an [AnimatedVisibilityScope] for the given [OverlayState]. */
@OptIn(ExperimentalTransitionApi::class)
@Composable
private fun Transition<OverlayHostData<Any>?>.animatedVisibilityScope(
  visible: (OverlayHostData<Any>?) -> Boolean
): AnimatedVisibilityScope {
  val childTransition =
    createChildTransition(label = "Overlay transition") { overlayState ->
      targetEnterExit(visible, overlayState)
    }
  return remember(childTransition) { SimpleAnimatedVisibilityScope(childTransition) }
}

/** A [AnimatedVisibilityScope] that takes a [Transition]. */
private data class SimpleAnimatedVisibilityScope(
  override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope

// This converts Boolean visible to EnterExitState
@Composable
private fun <T> Transition<T>.targetEnterExit(
  visible: (T) -> Boolean,
  targetState: T,
): EnterExitState =
  key(this) {
    if (this.isSeeking) {
      if (visible(targetState)) {
        Visible
      } else {
        if (visible(this.currentState)) {
          PostExit
        } else {
          PreEnter
        }
      }
    } else {
      val hasBeenVisible = remember { mutableStateOf(false) }
      if (visible(currentState)) {
        hasBeenVisible.value = true
      }
      if (visible(targetState)) {
        Visible
      } else {
        // If never been visible, visible = false means PreEnter, otherwise PostExit
        if (hasBeenVisible.value) {
          PostExit
        } else {
          PreEnter
        }
      }
    }
  }

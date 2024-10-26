// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sharedelements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

/**
 * [SharedElementTransitionLayout] creates a layout with a [SharedElementTransitionScope]. Any child
 * layout of [SharedElementTransitionLayout] can then use the receiver scope
 * [SharedElementTransitionScope] to create standard shared element or shared bounds transitions.
 *
 * Any indirect child layout of the [SharedElementTransitionLayout] can use the
 * [SharedElementTransitionScope] composable to access the [SharedElementTransitionScope] created by
 * this layout.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param content The child composable to be laid out.
 */
@ExperimentalSharedTransitionApi
@Composable
public fun SharedElementTransitionLayout(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  SharedTransitionScope { sharedTransitionModifier ->
    val scope = remember { SharedElementTransitionScopeImpl(this) }
    Box(
      modifier
        .then(sharedTransitionModifier)
        // Workaround for https://issuetracker.google.com/issues/344343033
        .layout { measurable, constraints ->
          val placeable = measurable.measure(constraints)
          layout(placeable.width, placeable.height) {
            if (coordinates != null && isLookingAhead) {
              scope.hasLayoutCoordinates = true
            }
            placeable.place(0, 0)
          }
        }
    ) {
      CompositionLocalProvider(
        LocalSharedElementTransitionScope provides scope,
        LocalSharedElementTransitionState provides SharedElementTransitionState.Available,
      ) {
        content()
      }
    }
  }
}

/** Represents the current state of the available [SharedElementTransitionScope]. */
public enum class SharedElementTransitionState {
  /** Indicates that shared element transitions are not available. */
  Unavailable,
  /** Indicates that shared element transitions are available. */
  Available,
}

/**
 * A [ProvidableCompositionLocal] to expose the current [SharedElementTransitionState] in the
 * composition tree.
 */
public val LocalSharedElementTransitionState:
  ProvidableCompositionLocal<SharedElementTransitionState> =
  compositionLocalOf {
    SharedElementTransitionState.Unavailable
  }

/**
 * Helper for previewing a [SharedElementTransitionLayout] while also providing an [AnimatedScope].
 *
 * @param modifier Modifier to be applied to the layout.
 * @param animatedScope The [AnimatedScope] to provide.
 * @param content The child composable to be laid out.
 */
@ExperimentalSharedTransitionApi
@Composable
public fun PreviewSharedElementTransitionLayout(
  modifier: Modifier = Modifier,
  animatedScope: AnimatedScope = Navigation,
  content: @Composable () -> Unit,
) {
  SharedElementTransitionLayout(modifier = modifier) {
    AnimatedVisibility(visible = true, enter = EnterTransition.None, exit = ExitTransition.None) {
      ProvideAnimatedTransitionScope(
        animatedScope = animatedScope,
        animatedVisibilityScope = this,
        content = content,
      )
    }
  }
}

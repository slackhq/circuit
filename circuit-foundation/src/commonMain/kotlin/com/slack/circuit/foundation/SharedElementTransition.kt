// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Overlay
import com.slack.circuit.runtime.InternalCircuitApi

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
@OptIn(InternalCircuitApi::class)
@Composable
public fun SharedElementTransitionLayout(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  SharedTransitionLayout(modifier = modifier) {
    val scope = remember { SharedElementTransitionScopeImpl(this) }
    CompositionLocalProvider(
      LocalSharedElementTransitionScope provides scope,
      LocalSharedElementTransitionState provides SharedElementTransitionState.Available,
    ) {
      content()
    }
  }
}

@ExperimentalSharedTransitionApi
@OptIn(InternalCircuitApi::class)
@Composable
public fun ProvideAnimatedTransitionScope(
  animatedScope: AnimatedScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  content: @Composable () -> Unit,
) {
  if (LocalSharedElementTransitionState.current == SharedElementTransitionState.Available) {
    val parent = LocalSharedElementTransitionScope.current
    val scope =
      remember(parent) { SharedElementTransitionScopeImpl(parent) }
        .apply { setScope(animatedScope, animatedVisibilityScope) }
    CompositionLocalProvider(LocalSharedElementTransitionScope provides scope) { content() }
  } else {
    content()
  }
}

/**
 * [SharedElementTransitionScope] creates a [SharedElementTransitionScope] for the child layouts in
 * [content]. Any child layout of [SharedElementTransitionScope] can then use the receiver scope
 * [SharedElementTransitionScope] to create standard shared element or shared bounds transitions.
 *
 * @param content The child composable to be laid out.
 */
@ExperimentalSharedTransitionApi
@OptIn(InternalCircuitApi::class)
@Composable
public fun SharedElementTransitionScope(
  sharedElementTransitionScope: SharedElementTransitionScope =
    LocalSharedElementTransitionScope.current,
  content: @Composable SharedElementTransitionScope.() -> Unit,
) {
  sharedElementTransitionScope.content()
}

/**
 * [SharedElementTransitionScope] provides a [SharedTransitionScope] for the standard shared
 * elements/shared bounds animations. This also provides a [AnimatedVisibilityScope], which can be
 * set using [set] and retrieved using [get] or [requireAnimatedScope]. Typically this should be set
 * by a [NavDecoration] that has children using [SharedElementTransitionScope] for the animations to
 * match up with the navigation animations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public interface SharedElementTransitionScope : SharedTransitionScope {

  public fun availableScopes(): Set<AnimatedScope>

  public fun getAnimatedScope(key: AnimatedScope): AnimatedVisibilityScope?

  public fun requireAnimatedScope(key: AnimatedScope): AnimatedVisibilityScope {
    return requireNotNull(getAnimatedScope(key)) { "No AnimatedVisibilityScope found for $key" }
  }

  public enum class AnimatedScope {
    Overlay,
    Navigation,
  }

  public companion object {
    @Composable
    public fun isAvailable(): Boolean {
      return LocalSharedElementTransitionState.current == SharedElementTransitionState.Available
    }
  }
}

/**
 * Dynamically switch between the [AnimatedScope.Overlay] and [AnimatedScope.Navigation] for shared
 * elements that can exist across Navigation and Overlay transitions.
 */
public fun SharedElementTransitionScope.requireActiveAnimatedScope(): AnimatedVisibilityScope {
  val scope = requireAnimatedScope(Overlay)
  val current = scope.transition.currentState
  val target = scope.transition.targetState
  // Visible -> PostExit - Hiding behind the overlay
  // PostExit -> PostExit - Hidden behind the overlay
  // PostExit -> Visible - Showing as the overlay is hidden
  return when {
    current == EnterExitState.Visible && target == EnterExitState.PostExit ||
      target == EnterExitState.PostExit && current == EnterExitState.PostExit ||
      current == EnterExitState.PostExit && target == EnterExitState.Visible -> scope
    else -> requireAnimatedScope(Navigation)
  }
}

/**
 * A provider of a [SharedTransitionScope] for a [SharedElementTransitionScope]. This should be set
 * by a [SharedElementTransitionLayout].
 *
 * By default this falls back to a no-op [SharedTransitionScope].
 */
@InternalCircuitApi
private val LocalSharedElementTransitionScope:
  ProvidableCompositionLocal<SharedElementTransitionScope> =
  compositionLocalOf {
    error("No SharedElementTransitionScope provided")
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

/** Represents the current state of the available [SharedElementTransitionScope]. */
public enum class SharedElementTransitionState {
  /** Indicates that shared element transitions are not available. */
  Unavailable,

  /** Indicates that shared element transitions are available. */
  Available,
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
    AnimatedVisibility(visible = true) {
      ProvideAnimatedTransitionScope(
        animatedScope = animatedScope,
        animatedVisibilityScope = this,
        content = content,
      )
    }
  }
}

/**
 * [SharedElementTransitionScope] implementation that delegates to a provided
 * [SharedTransitionScope] and allows for updating the [animatedVisibilityScopes].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private data class SharedElementTransitionScopeImpl(
  private val sharedTransitionScope: SharedTransitionScope
) : SharedElementTransitionScope, SharedTransitionScope by sharedTransitionScope {

  private val parentScope = sharedTransitionScope as? SharedElementTransitionScope
  private val animatedVisibilityScopes = mutableStateMapOf<AnimatedScope, AnimatedVisibilityScope>()

  fun setScope(key: AnimatedScope, value: AnimatedVisibilityScope) {
    animatedVisibilityScopes[key] = value
  }

  override fun availableScopes(): Set<AnimatedScope> {
    return animatedVisibilityScopes.keys + (parentScope?.availableScopes() ?: emptySet())
  }

  override fun getAnimatedScope(key: AnimatedScope): AnimatedVisibilityScope? {
    return animatedVisibilityScopes[key] ?: parentScope?.getAnimatedScope(key)
  }
}

/** Current progress fraction of the animation, between 0f and 1f. */
public fun AnimatedVisibilityScope.progress(): FloatState {
  return derivedStateOf {
      with(transition) {
        when {
          isRunning || isSeeking -> {
            val fraction = playTimeNanos * 1f / totalDurationNanos
            fraction.coerceIn(0f, 1f)
          }
          currentState == EnterExitState.Visible -> 1f
          else -> 0f
        }
      }
    }
    .asFloatState()
}

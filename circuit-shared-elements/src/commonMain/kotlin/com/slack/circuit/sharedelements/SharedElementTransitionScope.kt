// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sharedelements

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Overlay

/**
 * [SharedElementTransitionScope] provides a [SharedTransitionScope] for the standard shared
 * elements/shared bounds animations. This also provides a [AnimatedVisibilityScope], which can be
 * set using [set] and retrieved using [get] or [requireAnimatedScope]. In Circuit this is set by a
 * NavDecoration that has children using the [SharedElementTransitionScope] function for the
 * animations to match up with the navigation animations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public interface SharedElementTransitionScope : SharedTransitionScope {

  @DelicateCircuitSharedElementsApi public val hasLayoutCoordinates: Boolean

  /** Get the set of available [AnimatedScope]s for this [SharedElementTransitionScope]. */
  public fun availableScopes(): Set<AnimatedScope>

  /** Get the [AnimatedVisibilityScope] for the given [key], if it exists. */
  public fun getAnimatedScope(key: AnimatedScope): AnimatedVisibilityScope?

  /** Require the [AnimatedVisibilityScope] for the given [key]. Throwing if it does not. */
  public fun requireAnimatedScope(key: AnimatedScope): AnimatedVisibilityScope {
    return requireNotNull(getAnimatedScope(key)) { "No AnimatedVisibilityScope found for $key" }
  }

  /**
   * A key used to set and retrieve an [AnimatedVisibilityScope] from the
   * [SharedElementTransitionScope].
   */
  public interface AnimatedScope {
    /** A [AnimatedScope] for shared element transitions with Circuit Overlays. */
    public object Overlay : AnimatedScope

    /** A [AnimatedScope] for shared element transitions with Circuit Navigation. */
    public object Navigation : AnimatedScope
  }

  public companion object {
    /** Helper to check if */
    @Composable
    public fun isAvailable(): Boolean {
      return LocalSharedElementTransitionState.current == SharedElementTransitionState.Available
    }
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
@Composable
public fun SharedElementTransitionScope(
  sharedElementTransitionScope: SharedElementTransitionScope =
    LocalSharedElementTransitionScope.current,
  content: @Composable SharedElementTransitionScope.() -> Unit,
) {
  sharedElementTransitionScope.content()
}

/**
 * [ProvideAnimatedTransitionScope] sets the [animatedVisibilityScope] as the given [animatedScope]
 * for the layouts in [content] to access from [SharedElementTransitionScope]. If no parent
 * [SharedElementTransitionScope] is [SharedElementTransitionState.Available] then the content is
 * shown normally.
 */
@ExperimentalSharedTransitionApi
@Composable
public fun ProvideAnimatedTransitionScope(
  animatedScope: AnimatedScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  content: @Composable () -> Unit,
) {
  if (SharedElementTransitionScope.isAvailable()) {
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
 * A provider of a [SharedTransitionScope] for a [SharedElementTransitionScope]. This should be set
 * by a [SharedElementTransitionLayout].
 */
internal val LocalSharedElementTransitionScope:
  ProvidableCompositionLocal<SharedElementTransitionScope> =
  compositionLocalOf {
    error("No SharedElementTransitionScope provided")
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
 * [SharedElementTransitionScope] implementation that delegates to a provided
 * [SharedTransitionScope] and allows for updating the [animatedVisibilityScopes].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal data class SharedElementTransitionScopeImpl(
  private val sharedTransitionScope: SharedTransitionScope
) : SharedElementTransitionScope, SharedTransitionScope by sharedTransitionScope {

  private val parentScope = sharedTransitionScope as? SharedElementTransitionScope
  private val animatedVisibilityScopes = mutableStateMapOf<AnimatedScope, AnimatedVisibilityScope>()

  override var hasLayoutCoordinates by mutableStateOf(false)
    internal set

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

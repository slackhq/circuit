// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavDecoration
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
  content: @Composable SharedElementTransitionScope.() -> Unit,
) {
  SharedTransitionLayout(modifier = modifier) {
    val sharedTransitionScope = this
    val animatedVisibilityScope = findAnimatedVisibilityScope()
    CompositionLocalProvider(
      LocalSharedTransitionScope provides sharedTransitionScope,
      LocalTransitionAnimatedVisibilityScope provides animatedVisibilityScope,
    ) {
      SharedElementTransitionScope(sharedTransitionScope, animatedVisibilityScope, content)
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
@OptIn(InternalCircuitApi::class)
@Composable
public fun SharedElementTransitionScope(
  sharedTransitionScope: SharedTransitionScope = LocalSharedTransitionScope.current,
  animatedVisibilityScope: AnimatedVisibilityScope = findAnimatedVisibilityScope(),
  content: @Composable SharedElementTransitionScope.() -> Unit,
) {
  val scope =
    remember(sharedTransitionScope) {
      SharedElementTransitionScopeImpl(animatedVisibilityScope, sharedTransitionScope)
    }
  with(scope) {
    updateAnimatedScope(animatedVisibilityScope)
    content()
  }
}

/**
 * [SharedElementTransitionScope] provides a [SharedTransitionScope] for the standard shared
 * elements/shared bounds animations. This also provides a [AnimatedVisibilityScope], which can be
 * set using [LocalTransitionAnimatedVisibilityScope]. Typically this should be set by a
 * [NavDecoration] that has children using [SharedElementTransitionScope] for the animations to
 * match up with the navigation animations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public interface SharedElementTransitionScope : SharedTransitionScope {
  public val animatedVisibilityScope: AnimatedVisibilityScope

  public fun isTransitionState(predicate: (EnterExitState) -> Boolean): Boolean {
    return with(animatedVisibilityScope) {
      predicate(transition.targetState) || predicate(transition.currentState)
    }
  }
}

/**
 * Finds the [AnimatedVisibilityScope] from the [LocalTransitionAnimatedVisibilityScope] or creates
 * a [SimpleAnimatedVisibilityScope].
 */
@Composable
private fun findAnimatedVisibilityScope(): AnimatedVisibilityScope {
  return LocalTransitionAnimatedVisibilityScope.current ?: staticAnimatedVisibilityScope()
}

/**
 * Creates an [AnimatedVisibilityScope] that is always visible as a fallback for any
 * [SharedElementTransitionScope].
 */
@Composable
private fun staticAnimatedVisibilityScope(): AnimatedVisibilityScope {
  val transition =
    updateTransition(EnterExitState.Visible, label = "Static AnimatedVisibilityScope")
  return remember { SimpleAnimatedVisibilityScope(transition) }
}

/** A [AnimatedVisibilityScope] that takes a [Transition]. */
private data class SimpleAnimatedVisibilityScope(
  override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope

/**
 * A provider of a [SharedTransitionScope] for a [SharedElementTransitionScope]. This should be set
 * by a [SharedElementTransitionLayout].
 *
 * By default this falls back to a no-op [SharedTransitionScope].
 */
@InternalCircuitApi
@OptIn(ExperimentalSharedTransitionApi::class)
public val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope> =
  compositionLocalOf {
    error("No SharedTransitionScope provided")
  }

/**
 * This provides a [AnimatedVisibilityScope] to any [SharedElementTransitionScope]. Typically this
 * should be set by a [NavDecoration] so that shared transitions can match up with the any
 * navigation animations.
 */
public val LocalTransitionAnimatedVisibilityScope:
  ProvidableCompositionLocal<AnimatedVisibilityScope?> =
  compositionLocalOf {
    null
  }

/**
 * [SharedElementTransitionScope] implementation that delegates to a provided
 * [SharedTransitionScope] and allows for updating the [animatedVisibilityScope].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private data class SharedElementTransitionScopeImpl(
  private val animatedScope: AnimatedVisibilityScope,
  private val sharedTransitionScope: SharedTransitionScope,
) : SharedElementTransitionScope, SharedTransitionScope by sharedTransitionScope {

  private val animatedVisibilityScopeState = mutableStateOf(animatedScope)

  override val animatedVisibilityScope: AnimatedVisibilityScope
    get() = animatedVisibilityScopeState.value

  /** Update the [animatedVisibilityScope] to [animatedScope]. */
  public fun updateAnimatedScope(animatedScope: AnimatedVisibilityScope) {
    animatedVisibilityScopeState.value = animatedScope
  }
}

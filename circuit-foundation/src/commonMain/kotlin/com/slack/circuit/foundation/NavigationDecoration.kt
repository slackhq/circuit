// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.AnimatedNavigationTransform.Direction
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlinx.collections.immutable.ImmutableList

/**
 * todo Finish docs.
 *
 * Animated navigation decoration is an implementation of [NavDecoration] that provides the
 * [Navigation] [AnimatedVisibilityScope] for shared elements. It also allows for indepth
 * customization of its [AnimatedContent] through the [AnimatedNavDecorator].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public class AnimatedNavDecoration(
  private val animatedNavOverrides: ImmutableList<AnimatedNavigationTransform>,
  private val decoratorFactory: AnimatedNavDecorator.Factory,
) : NavDecoration {

  @Composable
  public override fun <T : NavArgument> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val decorator = remember {
      @Suppress("UNCHECKED_CAST")
      decoratorFactory.create<T>() as AnimatedNavDecorator<T, AnimatedNavState>
    }
    with(decorator) {
      val transition = updateTransition(args, backStackDepth)
      transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = transitionSpec(animatedNavOverrides),
      ) { targetState ->
        ProvideAnimatedTransitionScope(Navigation, this) { Decoration(targetState) { content(it) } }
      }
    }
  }
}

/**
 * todo Finish docs.
 *
 * [AnimatedNavDecorator] is used by [AnimatedNavDecoration] as a way to provide hooks into its
 * [AnimatedContent], allowing for custom navigation animations.
 */
@Stable
public interface AnimatedNavDecorator<T : NavArgument, S : AnimatedNavState> {

  /** Builds the default [AnimatedContent] transition spec. */
  public val defaultTransform: DefaultAnimatedNavigationTransform

  /** For the args and backstack create the expected target [AnimatedNavState]. */
  public fun targetState(args: ImmutableList<T>, backStackDepth: Int): S

  /**
   * Sets up a [Transition] for driving an [AnimatedContent] used to navigate between screens. The
   * transition should be setup from the current [NavDecoration.DecoratedContent] arguments and
   * backstack depth, and then updated when the arguments or backstack depth change.
   */
  @Composable
  public fun updateTransition(args: ImmutableList<T>, backStackDepth: Int): Transition<S>

  /** For the [targetState], decorate the [innerContent] as the `content` of [AnimatedContent] */
  @Composable
  public fun AnimatedContentScope.Decoration(targetState: S, innerContent: @Composable (T) -> Unit)

  @Stable
  public interface Factory {

    public fun <T : NavArgument> create(): AnimatedNavDecorator<T, *>
  }
}

/** A state created for the [Transition] in [AnimatedNavDecorator.Decoration]. */
@Stable
public interface AnimatedNavState {
  /** The [Screen] associated with this state. */
  public val screen: Screen

  /** The root screen of the back stack at the time this state was created. */
  public val rootScreen: Screen

  /** The depth of the back stack at the time this state was created. */
  public val backStackDepth: Int
}

public interface DefaultAnimatedNavigationTransform : AnimatedNavigationTransform {
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    direction: Direction,
    sameRoot: Boolean,
  ): ContentTransform
}

// todo
public interface AnimatedNavigationTransform {

  /**
   * Based on the source state, target state, and direction, return a [ContentTransform] to override
   * the transition spec in the [AnimatedNavDecoration] [AnimatedContent].
   *
   * Used as a way to provide a custom [ContentTransform] for specific navigation state changes
   * while using [AnimatedNavDecorator.defaultTransform] as the overall default.
   *
   * @return The [ContentTransform] to use or null if no override is desired.
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    direction: Direction,
    sameRoot: Boolean,
  ): ContentTransform?

  // todo Change "Direction" as the name, want to indicate root changes and same diff
  public enum class Direction {
    Forward, // Initial -> Target
    Backward, // Target was the previous Initial
    Unknown, // Either the root screen has changed or the backstack depth is the same.
  }
}

@Composable
private fun <T : NavArgument> AnimatedNavDecorator<T, AnimatedNavState>.transitionSpec(
  animatedNavOverrides: ImmutableList<AnimatedNavigationTransform>
): AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform = {
  val diff = targetState.backStackDepth - initialState.backStackDepth
  val sameRoot = targetState.rootScreen == initialState.rootScreen
  val direction =
    when {
      sameRoot && diff > 0 -> Direction.Forward
      sameRoot && diff < 0 -> Direction.Backward
      else -> Direction.Unknown
    }
  animatedNavigationOverride(animatedNavOverrides, direction, sameRoot)
    ?: with(defaultTransform) { transitionSpec(direction, sameRoot) }
}

private fun AnimatedContentTransitionScope<AnimatedNavState>.animatedNavigationOverride(
  animatedNavOverrides: ImmutableList<AnimatedNavigationTransform>,
  direction: Direction,
  sameRoot: Boolean,
): ContentTransform? {
  for (navigationTransform in animatedNavOverrides) {
    val transform = with(navigationTransform) { transitionSpec(direction, sameRoot) }
    if (transform != null) {
      return transform
    }
  }
  return null
}

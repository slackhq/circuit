// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.AnimatedNavigationTransform.NavigationEvent
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
  public val defaultTransform: RequiredAnimatedNavigationTransform

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

/**
 * A non-nullable implementation of [AnimatedNavigationTransform]. Is the default [ContentTransform]
 * provided by a [AnimatedNavDecorator] to the [AnimatedNavDecoration].
 */
public interface RequiredAnimatedNavigationTransform : AnimatedNavigationTransform {
  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    navigationEvent: NavigationEvent
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
   * @receiver Requires a [AnimatedContentTransitionScope] to access the [Transition.currentState]
   *   and [Transition.targetState]. Also allows for use of the properties on
   *   [AnimatedContentTransitionScope], while building the [ContentTransform].
   */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    navigationEvent: NavigationEvent
  ): ContentTransform?

  public enum class NavigationEvent {
    /** Going to the [Transition.targetState] from the [Transition.currentState] */
    GoTo,
    /** Popping from the [Transition.currentState] back to to the [Transition.targetState] */
    Pop,
    /** The back stack has been reset to the [Transition.targetState]. */
    RootReset,
  }
}

@Composable
private fun <T : NavArgument> AnimatedNavDecorator<T, AnimatedNavState>.transitionSpec(
  animatedNavOverrides: ImmutableList<AnimatedNavigationTransform>
): AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform = spec@{
  val diff = targetState.backStackDepth - initialState.backStackDepth
  val sameRoot = targetState.rootScreen == initialState.rootScreen
  val navigationEvent =
    when {
      !sameRoot -> NavigationEvent.RootReset
      diff > 0 -> NavigationEvent.GoTo
      diff < 0 -> NavigationEvent.Pop
      // Somehow the back stack has not changed?
      else -> return@spec EnterTransition.None togetherWith ExitTransition.None
    }
  val override = animatedNavigationOverride(animatedNavOverrides, navigationEvent)
  override ?: with(defaultTransform) { transitionSpec(navigationEvent) }
}

private fun AnimatedContentTransitionScope<AnimatedNavState>.animatedNavigationOverride(
  animatedNavOverrides: ImmutableList<AnimatedNavigationTransform>,
  navigationEvent: NavigationEvent,
): ContentTransform? {
  for (navigationTransform in animatedNavOverrides) {
    val transform = with(navigationTransform) { transitionSpec(navigationEvent) }
    if (transform != null) {
      return transform
    }
  }
  return null
}

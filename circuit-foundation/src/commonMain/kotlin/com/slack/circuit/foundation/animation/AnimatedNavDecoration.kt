// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavigationTransform.NavigationEvent
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlinx.collections.immutable.ImmutableList

/**
 * Animated navigation decoration is an implementation of [NavDecoration] that provides the
 * [Navigation] [AnimatedVisibilityScope] for shared elements. It also allows for indepth
 * customization of its [AnimatedContent] through a [AnimatedNavDecorator].
 *
 * **How this works:**
 * - The [DecoratedContent] method is called to render content by the Navigation system in Circuit,
 *   given the current navigation state.
 * - The [decoratorFactory] is called to obtain and remember an [AnimatedNavDecorator] instance.
 * - On the obtained decorator, [AnimatedNavDecorator.updateTransition] is called passing the
 *   current navigation arguments and back stack depth.
 * - An [AnimatedContent] container is then created on the returned [Transition].
 * - Using the decorator and [transforms], build the [AnimatedContent] `transitionSpec`. This will
 *   compare each of the available transforms and select the first one that is able to handle the
 *   current transition. If no `AnimatedNavigationTransform` is able to handle the transition, then
 *   the default transition provided by the `AnimatedNavDecorator.defaultTransform` will be used.
 * - Then for each of the applicable [AnimatedContent] states the [Navigation]
 *   [SharedElementTransitionScope] is created and the [AnimatedNavDecorator.Decoration] is used to
 *   render the content.
 *
 * @param transforms A list of [AnimatedNavigationTransform] that might be used to override the
 *   default [ContentTransform] provided by the [AnimatedNavDecorator].
 * @param decoratorFactory A factory used to create a [AnimatedNavDecorator] instance.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public class AnimatedNavDecoration(
  private val transforms: ImmutableList<AnimatedNavigationTransform>,
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
        transitionSpec = transitionSpec(transforms),
      ) { targetState ->
        ProvideAnimatedTransitionScope(Navigation, this) { Decoration(targetState) { content(it) } }
      }
    }
  }
}

/**
 * Constructs the transition specification used in [AnimatedNavDecoration].
 *
 * This logic is used to select which type of animation to be used by the [AnimatedContent]. It uses
 * the [AnimatedNavigationTransform] to decide on this custom animation.
 *
 * **How this works:**
 * - First calculates the [AnimatedNavigationTransform.NavigationEvent] based on the difference
 *   between the `targetState` and `initialState`.
 * - After determining the [NavigationEvent] then look for an applicable
 *   [AnimatedNavigationTransform] in the [animatedNavigationTransforms] list.
 * - If a [AnimatedNavigationTransform.transitionSpec] returns a non-null [ContentTransform], that
 *   transition is used.
 * - If no applicable transform is found the one defined by [AnimatedNavDecorator.defaultTransform]
 *   is used.
 *
 * @param animatedNavigationTransforms A list of [AnimatedNavigationTransform]s that can be used to
 *   override the default transition.
 * @return A lambda that constructs the transition specification.
 */
@Composable
private fun <T : NavArgument> AnimatedNavDecorator<T, AnimatedNavState>.transitionSpec(
  animatedNavigationTransforms: ImmutableList<AnimatedNavigationTransform>
): AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform = spec@{
  // Order of overrides
  // 1. Any navigation context
  // 2. Any overrides
  // 3. Default
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
  val override = animatedNavigationOverride(animatedNavigationTransforms, navigationEvent)
  val baseTransform = override ?: with(defaultTransform) { transitionSpec(navigationEvent) }

  // todo Bind an AnimatedNavContext for a specific screen and update the base transform with it
  contextualNavigationOverride(baseTransform)
}

private fun AnimatedContentTransitionScope<AnimatedNavState>.contextualNavigationOverride(
  baseTransform: ContentTransform
): ContentTransform {
  // If we only pull from the target then the caller needs to apply the transitions on pop as well
  val targetContext = targetState.context.tag<AnimatedNavContext>()
  targetContext?.transform?.let {
    return it
  }
  return targetContext?.run {
    with(baseTransform) {
      val enter = enterTransition ?: targetContentEnter
      val exit = exitTransition ?: initialContentExit
      ContentTransform(enter, exit, targetContentZIndex, sizeTransform)
    }
  } ?: baseTransform
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

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
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

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
 * - Using the decorator and [animatedScreenTransforms], build the [AnimatedContent]
 *   `transitionSpec`. This will compare each of the available transforms and select the first one
 *   that is able to handle the current transition. If no `AnimatedNavigationTransform` is able to
 *   handle the transition, then the default transition provided by the
 *   `AnimatedNavDecorator.defaultTransform` will be used.
 * - Then for each of the applicable [AnimatedContent] states the [Navigation]
 *   [SharedElementTransitionScope] is created and the [AnimatedNavDecorator.Decoration] is used to
 *   render the content.
 *
 * @param animatedScreenTransforms A Map of [AnimatedScreenTransform] that might be used to override
 *   the default [ContentTransform] provided by the [AnimatedNavDecorator].
 * @param decoratorFactory A factory used to create a [AnimatedNavDecorator] instance.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public class AnimatedNavDecoration(
  private val animatedScreenTransforms: ImmutableMap<KClass<Screen>, AnimatedScreenTransform>,
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
        transitionSpec = transitionSpec(animatedScreenTransforms),
      ) { targetState ->
        ProvideAnimatedTransitionScope(Navigation, this) { Decoration(targetState) { content(it) } }
      }
    }
  }
}

/** Constructs the transition specification used in [AnimatedNavDecoration]. */
@Composable
private fun <T : NavArgument> AnimatedNavDecorator<T, AnimatedNavState>.transitionSpec(
  animatedScreenTransforms: ImmutableMap<KClass<Screen>, AnimatedScreenTransform>
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

  val baseTransform = transitionSpec(navigationEvent)
  val screenOverride = screenSpecificOverride(navigationEvent, animatedScreenTransforms)
  val navigationOverride = navigationSpecificOverride(navigationEvent)
  contextualNavigationOverride(baseTransform, screenOverride, navigationOverride)
}

private fun AnimatedContentTransitionScope<AnimatedNavState>.screenSpecificOverride(
  navigationEvent: NavigationEvent,
  animatedScreenTransforms: Map<KClass<Screen>, AnimatedScreenTransform>,
): PartialContentTransform {
  // Read any screen specific overrides
  val targetAnimatedScreenTransform =
    animatedScreenTransforms[targetState.screen::class] ?: NoOpAnimatedScreenTransform
  val initialAnimatedScreenTransform =
    animatedScreenTransforms[initialState.screen::class] ?: NoOpAnimatedScreenTransform

  return PartialContentTransform(
    enter = targetAnimatedScreenTransform.run { enterTransition(navigationEvent) },
    exit = initialAnimatedScreenTransform.run { exitTransition(navigationEvent) },
    zIndex = targetAnimatedScreenTransform.run { zIndex(navigationEvent) },
    sizeTransform = targetAnimatedScreenTransform.run { sizeTransform(navigationEvent) },
  )
}

private fun AnimatedContentTransitionScope<AnimatedNavState>.navigationSpecificOverride(
  navigationEvent: NavigationEvent
): PartialContentTransform {

  val targetContext = targetState.context.tag<AnimatedNavContext>()
  val initialContext = initialState.context.tag<AnimatedNavContext>()

  return when (navigationEvent) {
    NavigationEvent.GoTo -> {
      targetContext?.forward
    }
    NavigationEvent.Pop -> {
      // Read the forward value if it was set during pop, otherwise look for a context specified
      // from the goto call to this screen
      targetContext?.forward ?: initialContext?.reverse
    }
    NavigationEvent.RootReset -> {
      targetContext?.forward
    }
  } ?: PartialContentTransform.EMPTY
}

private fun contextualNavigationOverride(
  baseTransform: ContentTransform,
  screenOverride: PartialContentTransform,
  navigationOverride: PartialContentTransform,
): ContentTransform {
  // Call site takes precedent over the screen specific override
  return with(baseTransform) {
    val enter = navigationOverride.enter ?: screenOverride.enter ?: targetContentEnter
    val exit = navigationOverride.exit ?: screenOverride.exit ?: initialContentExit
    val zIndex = navigationOverride.zIndex ?: screenOverride.zIndex ?: targetContentZIndex
    val size = navigationOverride.sizeTransform ?: screenOverride.sizeTransform ?: sizeTransform
    ContentTransform(enter, exit, zIndex, size)
  }
}

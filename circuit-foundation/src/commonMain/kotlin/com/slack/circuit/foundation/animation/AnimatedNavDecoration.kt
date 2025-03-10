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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

/**
 * `AnimatedNavDecoration` is an implementation of [NavDecoration] that provides the `Navigation`
 * [AnimatedVisibilityScope] for shared elements. It also allows for indepth customization of its
 * [AnimatedContent] through a [AnimatedNavDecorator]. `AnimatedNavDecoration` is the default
 * `NavDecoration` used by Circuit.
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
 * ### Examples
 *
 * Using this `AnimatedNavDecorator.Factory` for the `CustomDecorator` in the [AnimatedNavDecorator]
 * example.
 *
 * ```kotlin
 * class CustomAnimatedNavDecoratorFactory() : AnimatedNavDecorator.Factory {
 *   override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
 *     return CustomDecorator()
 *   }
 * }
 * ```
 *
 * You can customize the default `AnimatedNavDecoration` when building a Circuit instance by
 * providing the `CustomAnimatedNavDecoratorFactory` to the `Circuit.Builder`. You can also add an
 * `AnimatedScreenTransform` for a specific `Screen`.
 *
 * ```kotlin
 * Circuit.Builder()
 *   .addUiFactories()
 *   .addPresenterFactories()
 *   .addAnimatedScreenTransform(CustomScreen::class, CustomScreenAnimatedTransform)
 *   .setAnimatedNavDecoratorFactory(CustomAnimatedNavDecoratorFactory())
 *   .build()
 * ```
 *
 * You can also customize the `AnimatedNavDecoration` for a specific `NavigableCircuitContent` by
 * providing it with an `AnimatedNavDecorator.Factory`. The provided `AnimatedNavDecorator.Factory`
 * will override any existing `NavDecoration` for that `NavigableCircuitContent`.
 *
 * ```kotlin
 * NavigableCircuitContent(
 *   navigator = navigator,
 *   backStack = backStack,
 *   decoratorFactory = remember { CustomAnimatedNavDecoratorFactory() },
 * )
 * ```
 *
 * @param animatedScreenTransforms A Map of [AnimatedScreenTransform] that might be used to override
 *   the default [ContentTransform] provided by the [AnimatedNavDecorator].
 * @param decoratorFactory A factory used to create a [AnimatedNavDecorator] instance.
 * @see AnimatedNavDecorator
 * @see AnimatedScreenTransform
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCircuitApi::class)
public class AnimatedNavDecoration(
  private val animatedScreenTransforms: ImmutableMap<KClass<out Screen>, AnimatedScreenTransform>,
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
      decoratorFactory.create<T>() as AnimatedNavDecorator<T, AnimatedNavState<T>>
    }
    with(decorator) {
      val transition = updateTransition(args, backStackDepth)
      println("AND ${transition.currentState.log()} -> ${transition.targetState.log()}")
      transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = transitionSpec(animatedScreenTransforms),
        contentKey = { it.navArgument.key },
      ) { targetState ->
        DisposableEffect(targetState) {
          println("AND Entered ${targetState.log()}")
          onDispose { println("AND Disposed ${targetState.log()}") }
        }
        ProvideAnimatedTransitionScope(Navigation, this) { Decoration(targetState) { content(it) } }
      }
    }
  }
}

/** Constructs the transition specification used in [AnimatedNavDecoration]. */
@OptIn(ExperimentalCircuitApi::class)
@Composable
private fun <T : NavArgument, S : AnimatedNavState<T>> AnimatedNavDecorator<T, S>.transitionSpec(
  animatedScreenTransforms: ImmutableMap<KClass<out Screen>, AnimatedScreenTransform>
): AnimatedContentTransitionScope<AnimatedContentState<T, S>>.() -> ContentTransform = spec@{
  val diff = targetState.backStackDepth - initialState.backStackDepth
  val sameRoot = targetState.rootScreen == initialState.rootScreen
  val animatedNavEvent =
    when {
      !sameRoot -> AnimatedNavEvent.RootReset
      diff > 0 -> AnimatedNavEvent.GoTo
      diff < 0 -> AnimatedNavEvent.Pop
      // Somehow the back stack has not changed?
      else -> return@spec EnterTransition.None togetherWith ExitTransition.None
    }

  val baseTransform = transitionSpec(animatedNavEvent)
  val screenOverride = screenSpecificOverride(animatedNavEvent, animatedScreenTransforms)
  contextualNavigationOverride(baseTransform, screenOverride)
}

@OptIn(ExperimentalCircuitApi::class)
private fun <T : NavArgument, S : AnimatedNavState<T>> TransitionScope<T, S>.screenSpecificOverride(
  animatedNavEvent: AnimatedNavEvent,
  animatedScreenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
): PartialContentTransform {
  val initialScreen = initialState.screen
  val targetScreen = targetState.screen
  // Read any screen specific overrides
  val targetAnimatedScreenTransform =
    animatedScreenTransforms[targetScreen::class] ?: NoOpAnimatedScreenTransform
  val initialAnimatedScreenTransform =
    animatedScreenTransforms[initialScreen::class] ?: NoOpAnimatedScreenTransform
  @Suppress("UNCHECKED_CAST")
  return with(this as TransitionScope<NavArgument, AnimatedNavState<NavArgument>>) {
    PartialContentTransform(
      enter = targetAnimatedScreenTransform.run { enterTransition(animatedNavEvent) },
      exit = initialAnimatedScreenTransform.run { exitTransition(animatedNavEvent) },
      zIndex = targetAnimatedScreenTransform.run { zIndex(animatedNavEvent) },
      sizeTransform = targetAnimatedScreenTransform.run { sizeTransform(animatedNavEvent) },
    )
  }
}

private fun contextualNavigationOverride(
  baseTransform: ContentTransform,
  screenOverride: PartialContentTransform,
): ContentTransform {
  // Screen specific override takes precedence over the AnimatedNavDecorator baseTransform
  return with(baseTransform) {
    val enter = screenOverride.enter ?: targetContentEnter
    val exit = screenOverride.exit ?: initialContentExit
    val zIndex = screenOverride.zIndex ?: targetContentZIndex
    val size = screenOverride.sizeTransform ?: sizeTransform
    ContentTransform(enter, exit, zIndex, size)
  }
}

// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/**
 * [AnimatedNavDecorator] is used by [AnimatedNavDecoration] as a way to provide hooks into its
 * [AnimatedContent], allowing for custom navigation animations.
 *
 * This interface allows you to define how the content of a navigation destination should appear and
 * animate when navigated to. It provides hooks into key moments of the navigation transition
 * process, enabling a high degree of customization.
 *
 * ### Usage
 *
 * **Implement `transitionSpec`**
 * - Provide a `ContentTransform` instance. This defines the default transition behavior used when
 *   no other override is provided.
 *
 * **Implement `targetState`**
 * - This is called to create an `AnimatedNavState` object for the incoming navigation destination.
 * - It should use the provided `args` and `backStackDepth` to construct an instance of a custom
 *   `AnimatedNavState` that represents the target state of the navigation.
 *
 * **Implement `updateTransition`**
 * - This is responsible for setting up and updating the [Transition] that drives the
 *   `AnimatedContent` that performs the navigation transition.
 * - You should create a [Transition] based on the provided `args` and `backStackDepth` to track the
 *   current state.
 * - The created `Transition` will be used by [AnimatedNavDecoration] to display the correct
 *   content.
 *
 * **Implement `Decoration`**
 * - This is a composable that renders the content for the target navigation destination.
 * - It receives the created `targetState` and an `innerContent` composable to render the actual
 *   screen's content.
 * - It will be used to compose the ui in the [AnimatedContent].
 *
 * ### Example
 *
 * ```kotlin
 * data class CustomNavState<T : NavArgument>(
 *   val args: ImmutableList<T>,
 *   override val backStackDepth: Int,
 *   override val screen: Screen = args.first().screen,
 *   override val rootScreen: Screen = args.last().screen,
 * ) : AnimatedNavState
 *
 * class CustomDecorator<T : NavArgument>() : AnimatedNavDecorator<T, CustomNavState<T>> {
 *
 *   override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
 *     animatedNavEvent: AnimatedNavEvent
 *   ): ContentTransform {
 *     return slideInVertically() + fadeIn() togetherWith slideOutVertically() + fadeOut()
 *   }
 *
 *   override fun targetState(args: ImmutableList<T>, backStackDepth: Int): CustomNavState<T> {
 *     // Logic to build your custom navigation state
 *     return CustomNavState(args, backStackDepth)
 *   }
 *
 *   @Composable
 *   override fun updateTransition(
 *     args: ImmutableList<T>,
 *     backStackDepth: Int,
 *   ): Transition<CustomNavState<T>> {
 *     val targetState = targetState(args, backStackDepth)
 *     return updateTransition(targetState = targetState, label = "CustomDecoratorTransition")
 *   }
 *
 *   @Composable
 *   override fun AnimatedContentScope.Decoration(
 *     targetState: CustomNavState<T>,
 *     innerContent: @Composable (T) -> Unit,
 *   ) {
 *     Box(modifier = Modifier.fillMaxSize()) { innerContent(targetState.args.first()) }
 *   }
 * }
 * ```
 *
 * @param T The type of navigation arguments.
 * @param S The type of navigation state, which must implement [AnimatedNavState].
 * @see AnimatedNavDecoration
 * @see AnimatedNavState
 * @see AnimatedScreenTransform
 */
@Stable
public interface AnimatedNavDecorator<T : NavArgument, S : AnimatedNavState> {
  /** For the args and backstack create the expected target [AnimatedNavState]. */
  public fun targetState(args: ImmutableList<T>, backStackDepth: Int): S

  /**
   * Sets up a [Transition] for driving an [AnimatedContent] used to navigate between screens. The
   * transition should be setup from the current [NavDecoration.DecoratedContent] arguments and
   * backstack depth, and then updated when the arguments or backstack depth change.
   */
  @Composable
  public fun updateTransition(args: ImmutableList<T>, backStackDepth: Int): Transition<S>

  /** Builds the default [AnimatedContent] transition spec. */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform

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

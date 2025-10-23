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
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen

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
 * - It should use the provided `args` to construct an instance of a custom `AnimatedNavState` that
 *   represents the target state of the navigation.
 *
 * **Implement `updateTransition`**
 * - This is responsible for setting up and updating the [Transition] that drives the
 *   `AnimatedContent` that performs the navigation transition.
 * - You should create a [Transition] based on the provided `args` to track the current state.
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
 *   override fun targetState(args: ImmutableList<T>): CustomNavState<T> {
 *     // Logic to build your custom navigation state
 *     return CustomNavState(args)
 *   }
 *
 *   @Composable
 *   override fun updateTransition(
 *     args: ImmutableList<T>,
 *   ): Transition<CustomNavState<T>> {
 *     val targetState = targetState(args)
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
  /** For the args create the expected target [AnimatedNavState]. */
  public fun targetState(args: List<T>): S

  /**
   * Sets up a [Transition] for driving an [AnimatedContent] used to navigate between screens. The
   * transition should be setup from the current [NavDecoration.DecoratedContent] arguments, and
   * then updated when the arguments change.
   */
  @Composable public fun updateTransition(args: List<T>): Transition<S>

  /** Builds the default [AnimatedContent] transition spec. */
  public fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform

  /** For the [targetState], decorate the [innerContent] as the `content` of [AnimatedContent] */
  @Composable
  public fun AnimatedContentScope.Decoration(targetState: S, innerContent: @Composable (T) -> Unit)

  @Stable
  public interface Factory {

    public fun <T : NavArgument> create(navigator: Navigator): AnimatedNavDecorator<T, *>
  }
}

/** A state created for the [Transition] in [AnimatedNavDecorator.Decoration]. */
@Stable
public interface AnimatedNavState {
  /** The [Screen] associated with this state. */
  public val top: NavArgument
    get() = backStack.first()

  /** The root screen of the back stack at the time this state was created. */
  public val root: NavArgument
    get() = backStack.last()

  /** The depth of the back stack at the time this state was created. */
  public val backStackDepth: Int
    get() = backStack.size

  /** Snapshot of the back stack at the time this state was created. */
  public val backStack: List<NavArgument>
}

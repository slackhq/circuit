// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
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
 * **Implement `defaultTransform`**
 * - Provide a `RequiredAnimatedNavigationTransform` instance. This defines the default transition
 *   behavior used when no other override is provided by a `AnimatedNavigationTransform`.
 *
 * **Implement `targetState`**
 * - This is called to create a `AnimatedNavState` object for the incoming navigation destination.
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
 * **Override Transitions (Optional)**
 * - When using an [AnimatedNavDecoration], you can optionally pass in a list of
 *   [AnimatedNavigationTransform]. This list can be used to override transitions based on the
 *   different [AnimatedNavigationTransform.NavigationEvent] that can occur during a transition.
 *
 * ### Example
 *
 * ```kotlin
 * data class CustomNavState(
 *   override val screen: Screen,
 *   override val rootScreen: Screen,
 *   override val backStackDepth: Int,
 * ) : AnimatedNavState
 *
 * class CustomDecorator<T : NavArgument>(private val rootScreen: Screen) :
 *   AnimatedNavDecorator<T, CustomNavState> {
 *
 *   override val defaultTransform: RequiredAnimatedNavigationTransform =
 *     object : RequiredAnimatedNavigationTransform {
 *       override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
 *         navigationEvent: AnimatedNavigationTransform.NavigationEvent
 *       ): ContentTransform {
 *         return slideInVertically() + fadeIn() togetherWith slideOutVertically() + fadeOut()
 *       }
 *     }
 *
 *   override fun targetState(args: ImmutableList<T>, backStackDepth: Int): CustomNavState {
 *     // Logic to build your custom navigation state
 *     val currentScreen = args.last() // Example
 *     return CustomNavState(currentScreen, rootScreen, backStackDepth)
 *   }
 *
 *   @Composable
 *   override fun updateTransition(
 *     args: ImmutableList<T>,
 *     backStackDepth: Int,
 *   ): Transition<CustomNavState> {
 *     val targetState = targetState(args, backStackDepth)
 *     return updateTransition(targetState = targetState, label = "CustomDecoratorTransition")
 *   }
 *
 *   @Composable
 *   override fun AnimatedContentScope.Decoration(
 *     targetState: CustomNavState,
 *     innerContent: @Composable (T) -> Unit,
 *   ) {
 *     Box(modifier = Modifier.fillMaxSize()) { innerContent(targetState.screen) }
 *   }
 * }
 * ```
 *
 * @param T The type of navigation arguments.
 * @param S The type of navigation state, which must implement [AnimatedNavState].
 * @see AnimatedNavDecoration
 * @see AnimatedNavState
 * @see AnimatedNavigationTransform
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

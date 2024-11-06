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
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
public abstract class AnimatedNavDecoration(
  private val decoratorFactory: AnimatedNavDecorator.Factory
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
      Content(args, backStackDepth, modifier) { modifier ->
        val backStackEntryState = sharedElementBackStack()
        AnimatedContent(modifier = modifier, transitionSpec = transitionSpec()) { targetState ->
          val animatedContentScope = this@AnimatedContent
          ProvideAnimatedTransitionScope(Navigation, animatedContentScope) {
            val sharedElementTransitionScope = this@ProvideAnimatedTransitionScope
            val entry = backStackEntryState.value
            val animatedModifier =
              if (sharedElementTransitionScope != null && entry != null) {
                Modifier.overrideSharedElementAnimations(
                  transitionScope = sharedElementTransitionScope,
                  animatedVisibilityScope = animatedContentScope,
                  state =
                    sharedElementTransitionScope.rememberSharedContentState(
                      NavigationSharedContentKey(entry)
                    ),
                  screen = targetState.screen,
                )
              } else {
                Modifier.overrideAnimations(animatedContentScope, targetState.screen)
              }
            Box(modifier = modifier.then(animatedModifier)) {
              AnimatedNavContent(targetState) { content(it) }
            }
          }
        }
      }
    }
  }
}

@Stable
public interface AnimatedNavDecorator<T : NavArgument, S : AnimatedNavState> {

  @Composable
  public fun Content(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable Transition<S>.(Modifier) -> Unit,
  )

  @Composable
  public fun Transition<S>.transitionSpec():
    AnimatedContentTransitionScope<S>.() -> ContentTransform

  @Composable
  public fun AnimatedContentScope.AnimatedNavContent(
    targetState: S,
    content: @Composable (T) -> Unit,
  )

  @Stable
  public interface Factory {

    public fun <T : NavArgument> create(): AnimatedNavDecorator<T, *>
  }
}

/** A state created for the [Transition] in [AnimatedNavDecorator.Content]. */
@Stable
public interface AnimatedNavState {
  /** The [Screen] associated with this state. */
  public val screen: Screen

  /** The depth of the back stack at the time this state was created. */
  public val backStackDepth: Int
}

/** A [Screen] that supports custom Enter/Exit transitions. */
public interface AnimatedScreen : Screen {
  /**
   * A [EnterTransition] to use when showing this screen. If null is returned the screen is animated
   * by the [NavDecoration].
   *
   * @param sharedElementTransition Whether this screen has a shared element transition.
   */
  public fun enterTransition(sharedElementTransition: Boolean = false): EnterTransition? = null

  /**
   * A [EnterTransition] to use when hiding this screen. If null is returned the screen is animated
   * by the [NavDecoration].
   *
   * @param sharedElementTransition Whether this screen has a shared element transition.
   */
  public fun exitTransition(sharedElementTransition: Boolean = false): ExitTransition? = null

  /**
   * A key for use when a shared element transition is expected when navigating to this screen. If
   * no shared element transition is expected, return `null`.
   */
  public fun sharedElementTransitionKey(): Any? = null
}

/** Create or retrieve a [SharedElementBackStackEntry] for the current back stack. */
@Composable
private fun Transition<AnimatedNavState>.sharedElementBackStack():
  State<SharedElementBackStackEntry?> {
  val current = targetState.asNavArgBackStackState()
  val previous = currentState.asNavArgBackStackState()

  val sharedKeys = rememberRetained {
    mutableStateMapOf<NavArgBackStackState, SharedElementBackStackEntry>()
  }

  var lastCurrent by rememberRetained { mutableStateOf(current) }
  var lastPrevious by rememberRetained { mutableStateOf(previous) }

  val lastCurrentValue = lastCurrent
  val lastPreviousValue = lastPrevious

  return produceState<SharedElementBackStackEntry?>(null, current, previous) {
    // Ignore a settled state
    if (current == previous) {
      value = null
      return@produceState
    }
    value =
      when {
        // Forward
        lastCurrentValue == previous &&
          (lastCurrentValue.backStackDepth + 1) == current.backStackDepth -> {
          forwardSharedElementBackStackEntry(current.screen, previous.screen)?.also {
            sharedKeys[current] = it
          }
        }
        // Backward
        lastPreviousValue == current &&
          lastPreviousValue.backStackDepth == current.backStackDepth -> {
          backwardSharedElementBackStackEntry(lastCurrentValue, current.screen, sharedKeys).also {
            sharedKeys.remove(lastCurrentValue)
          }
        }
        // Unknown
        else -> null
      }
    lastCurrent = current
    lastPrevious = previous
  }
}

/** If navigating forward, with a key, we're expecting a shared element transition. */
private fun forwardSharedElementBackStackEntry(
  current: Screen,
  previous: Screen,
): SharedElementBackStackEntry? {
  val currentScreen = current as? AnimatedScreen
  val key = currentScreen?.sharedElementTransitionKey() ?: return null
  return SharedElementBackStackEntry(key, currentScreen, previous)
}

/**
 * If navigating backward, with screens I've already seen and we have a key. We are expecting a
 * shared element transition.
 */
private fun backwardSharedElementBackStackEntry(
  lastCurrentArg: NavArgBackStackState,
  current: Screen,
  sharedKeys: SnapshotStateMap<NavArgBackStackState, SharedElementBackStackEntry>,
): SharedElementBackStackEntry? {
  val previousScreen = lastCurrentArg.screen as? AnimatedScreen
  val key = previousScreen?.sharedElementTransitionKey() ?: return null
  val entry = sharedKeys[lastCurrentArg] ?: return null
  return if (
    key == entry.key && previousScreen == entry.currentScreen && current == entry.previousScreen
  ) {
    entry
  } else {
    null
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun Modifier.overrideSharedElementAnimations(
  transitionScope: SharedElementTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  state: SharedContentState,
  screen: Screen,
): Modifier {
  val animatedScreen = screen as? AnimatedScreen
  val enter = animatedScreen?.enterTransition(sharedElementTransition = true)
  val exit = animatedScreen?.exitTransition(sharedElementTransition = true)
  if (enter == null || exit == null) return this
  return with(transitionScope) {
    with(animatedVisibilityScope) {
      then(Modifier.animateEnterExit(enter = enter, exit = exit))
        .sharedElement(state = state, animatedVisibilityScope = animatedVisibilityScope)
    }
  }
}

private fun Modifier.overrideAnimations(
  animatedVisibilityScope: AnimatedVisibilityScope,
  screen: Screen,
): Modifier {
  val animatedScreen = screen as? AnimatedScreen
  val enter = animatedScreen?.enterTransition(sharedElementTransition = false)
  val exit = animatedScreen?.exitTransition(sharedElementTransition = false)
  if (enter == null || exit == null) return this
  return with(animatedVisibilityScope) { animateEnterExit(enter = enter, exit = exit) }
}

private fun AnimatedNavState.asNavArgBackStackState(): NavArgBackStackState {
  return NavArgBackStackState(screen, backStackDepth)
}

private data class NavArgBackStackState(val screen: Screen, val backStackDepth: Int)

private data class SharedElementBackStackEntry(
  val key: Any,
  val currentScreen: AnimatedScreen,
  val previousScreen: Screen,
)

private data class NavigationSharedContentKey(val key: Any)

public class DefaultAnimatedNavDecoration(decoratorFactory: AnimatedNavDecorator.Factory) :
  AnimatedNavDecoration(decoratorFactory)

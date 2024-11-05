// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
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
      decoratorFactory.create<T>() as AnimatedNavDecorator<T, AnimatedState>
    }
    with(decorator) {
      Content(args, backStackDepth, modifier) { modifier ->
        AnimatedContent(modifier = modifier, transitionSpec = transitionSpec()) { targetState ->
          ProvideAnimatedTransitionScope(Navigation, this) {
            AnimatedNavContent(targetState) { content(it) }
          }
        }
      }
    }
  }
}

public class DefaultAnimatedNavDecoration(decoratorFactory: AnimatedNavDecorator.Factory) :
  AnimatedNavDecoration(decoratorFactory)

@Stable
public interface AnimatedNavDecorator<T : NavArgument, S : AnimatedState> {

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

public interface AnimatedState {
  public val screen: Screen
  public val backStackDepth: Int
}
public interface AnimatedScreen : Screen {
  public val enterTransition: EnterTransition?
  public val exitTransition: ExitTransition?
}

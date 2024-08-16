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
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalSharedTransitionApi::class)
public abstract class AnimatedNavDecoration(
  private val decoratorFactory: AnimatedNavDecorator.Factory
) : NavDecoration {

  @Composable
  public override fun <T> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {

    val decorator = remember {
      @Suppress("UNCHECKED_CAST")
      decoratorFactory.create<T>() as AnimatedNavDecorator<T, Any>
    }
    with(decorator) {
      Content(args, backStackDepth, modifier) { modifier ->
        AnimatedContent(modifier = modifier, transitionSpec = transitionSpec()) { targetState ->
          ProvideAnimatedTransitionScope(Navigation, this) {
            AnimatedContent(targetState) { content(it) }
          }
        }
      }
    }
  }
}

@Stable
public interface AnimatedNavDecorator<T, S> {

  @Composable
  public fun Content( // Behind, Chrome, Background, etc
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable Transition<S>.(Modifier) -> Unit,
  )

  @Composable
  public fun Transition<S>.transitionSpec():
    AnimatedContentTransitionScope<S>.() -> ContentTransform

  @Composable
  public fun AnimatedContentScope.AnimatedContent(targetState: S, content: @Composable (T) -> Unit)

  public interface Factory {

    public fun <T> create(): AnimatedNavDecorator<T, *>
  }
}

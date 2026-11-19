// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Wraps the decorators of [delegate] so that a pane plays [replaceTransform] when its bottom screen
 * is replaced, like when the user picks another list item while a detail is open.
 *
 * A horizontal push suits one pane that covers the window. Beside a list, the new detail would
 * slide in over the old one from the pane edge. The default [replaceTransform] moves the new detail
 * up a short distance while it fades in.
 *
 * Every other transition comes from [delegate]. This includes the first detail that opens, a pop,
 * and screens pushed over a detail. An `AnimatedScreenTransform` of a screen still takes priority.
 *
 * Use it in the [AnimatedNavDecoration] that a [PaneNavDecoration] wraps:
 * ```kotlin
 * AnimatedNavDecoration(
 *   animatedScreenTransforms = circuit.animatedScreenTransforms,
 *   decoratorFactory = PaneAnimatedNavDecoratorFactory(circuit.animatedNavDecoratorFactory),
 * )
 * ```
 *
 * [rememberPaneNavigation] does this for its default delegate. Outside a [PaneNavDecoration], the
 * factory changes nothing.
 *
 * @param delegate Creates the decorators that draw the screens and supply the other transitions.
 * @param replaceTransform The transition from the replaced screen to the new one.
 */
@ExperimentalCircuitAdaptiveApi
public class PaneAnimatedNavDecoratorFactory(
  private val delegate: AnimatedNavDecorator.Factory,
  private val replaceTransform:
    AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform =
    DefaultPaneReplaceTransform,
) : AnimatedNavDecorator.Factory {
  override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
    @Suppress("UNCHECKED_CAST")
    val decorator = delegate.create<T>() as AnimatedNavDecorator<T, AnimatedNavState>
    return PaneAnimatedNavDecorator(decorator, replaceTransform)
  }

  override fun equals(other: Any?): Boolean {
    return other is PaneAnimatedNavDecoratorFactory &&
      delegate == other.delegate &&
      replaceTransform == other.replaceTransform
  }

  override fun hashCode(): Int {
    return 31 * delegate.hashCode() + replaceTransform.hashCode()
  }
}

/** Passes every call to [delegate], and swaps the transition of a pane replace. */
private class PaneAnimatedNavDecorator<T : NavArgument, S : AnimatedNavState>(
  private val delegate: AnimatedNavDecorator<T, S>,
  private val replaceTransform:
    AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform,
) : AnimatedNavDecorator<T, S> {
  override fun updateNavigator(navigator: Navigator) {
    delegate.updateNavigator(navigator)
  }

  override fun targetState(args: NavStackList<T>): S {
    return delegate.targetState(args)
  }

  @Composable
  override fun updateTransition(args: NavStackList<T>): Transition<S> {
    return delegate.updateTransition(args)
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return if (
      animatedNavEvent == AnimatedNavEvent.GoTo &&
        isPaneReplace(initialState.navStack, targetState.navStack)
    ) {
      replaceTransform()
    } else {
      with(delegate) { transitionSpec(animatedNavEvent) }
    }
  }

  @Composable
  override fun AnimatedContentScope.Decoration(
    targetState: S,
    innerContent: @Composable (T) -> Unit,
  ) {
    with(delegate) { Decoration(targetState, innerContent) }
  }
}

/**
 * Whether the change from [initial] to [target] puts a new bottom screen in a pane that already
 * showed a screen. Both stacks must come from a split [PaneNavDecoration]. A screen from the single
 * layout does not count, so a layout switch keeps the transition of the delegate.
 */
internal fun isPaneReplace(
  initial: NavStackList<out NavArgument>,
  target: NavStackList<out NavArgument>,
): Boolean {
  val from = initial.active as? PaneArg.Record<*> ?: return false
  val to = target.active as? PaneArg.Record<*> ?: return false
  val toIsPaneRoot = to.role == PaneRole.DETAIL_ROOT || to.role == PaneRole.EXTRA_ROOT
  return toIsPaneRoot && from.role != PaneRole.SINGLE && from.key != to.key
}

/** The new screen fades in while it moves up a tenth of its height. The old screen fades out. */
internal val DefaultPaneReplaceTransform:
  AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform =
  {
    val enter =
      fadeIn(tween(durationMillis = 210, delayMillis = 90)) +
        slideInVertically(tween(durationMillis = 300)) { height -> height / 10 }
    enter togetherWith fadeOut(tween(durationMillis = 90))
  }

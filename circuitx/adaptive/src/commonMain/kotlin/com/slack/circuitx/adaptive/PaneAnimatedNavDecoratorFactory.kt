// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
 * Wraps the decorators of [delegate] so that the end pane of a [PaneNavDecoration] gets transitions
 * that suit a pane.
 *
 * A horizontal push suits one pane that covers the window. In a pane it looks wrong in two cases:
 * - A pane that opens already slides in from the end edge, as the decoration moves the pane. A push
 *   inside it would add a second motion. [openTransform] replaces it, and by default it does
 *   nothing, so the screen rides in with its pane.
 * - A screen that replaces the bottom screen of a pane, like another list item, would slide over
 *   the old one. [replaceTransform] replaces it, and by default it is a crossfade with a small
 *   scale. This also applies when the first screen opens over a placeholder that the app draws.
 *
 * Every other transition comes from [delegate]. This includes a pop and screens pushed over a
 * detail. An `AnimatedScreenTransform` of a screen still takes priority.
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
 * @param openTransform The transition of the first screen in a pane that was not visible.
 * @param replaceTransform The transition from the content of a visible pane to a new bottom screen.
 */
@ExperimentalCircuitAdaptiveApi
public class PaneAnimatedNavDecoratorFactory(
  private val delegate: AnimatedNavDecorator.Factory,
  private val openTransform:
    AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform =
    DefaultPaneOpenTransform,
  private val replaceTransform:
    AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform =
    DefaultPaneReplaceTransform,
) : AnimatedNavDecorator.Factory {
  override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
    @Suppress("UNCHECKED_CAST")
    val decorator = delegate.create<T>() as AnimatedNavDecorator<T, AnimatedNavState>
    return PaneAnimatedNavDecorator(decorator, openTransform, replaceTransform)
  }

  override fun equals(other: Any?): Boolean {
    return other is PaneAnimatedNavDecoratorFactory &&
      delegate == other.delegate &&
      openTransform == other.openTransform &&
      replaceTransform == other.replaceTransform
  }

  override fun hashCode(): Int {
    var result = delegate.hashCode()
    result = 31 * result + openTransform.hashCode()
    result = 31 * result + replaceTransform.hashCode()
    return result
  }
}

/** Passes every call to [delegate], and swaps the transitions of a pane open and a pane replace. */
private class PaneAnimatedNavDecorator<T : NavArgument, S : AnimatedNavState>(
  private val delegate: AnimatedNavDecorator<T, S>,
  private val openTransform:
    AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform,
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
    val change =
      if (animatedNavEvent == AnimatedNavEvent.GoTo) {
        paneChange(initialState.navStack, targetState.navStack)
      } else {
        null
      }
    return when (change) {
      PaneChange.Open -> openTransform()
      PaneChange.Replace -> replaceTransform()
      null -> with(delegate) { transitionSpec(animatedNavEvent) }
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

/** A change to the bottom screen of a pane that gets its own transition. */
internal enum class PaneChange {
  /** The first screen of a pane that was not visible. The decoration slides the pane in. */
  Open,

  /** A new bottom screen in a pane that already showed a screen or a placeholder. */
  Replace,
}

/**
 * Returns how the change from [initial] to [target] changes the bottom screen of a pane, or null
 * for any other change. Both stacks must come from a split [PaneNavDecoration]. A screen from the
 * single layout does not count, so a layout switch keeps the transition of the delegate.
 */
internal fun paneChange(
  initial: NavStackList<out NavArgument>,
  target: NavStackList<out NavArgument>,
): PaneChange? {
  val to = target.active as? PaneArg.Record<*> ?: return null
  if (to.role != PaneRole.DetailRoot && to.role != PaneRole.ExtraRoot) {
    return null
  }
  return when (val from = initial.active) {
    is PaneArg.Placeholder -> {
      if (from.shown) {
        PaneChange.Replace
      } else {
        PaneChange.Open
      }
    }
    is PaneArg.Record<*> -> {
      if (from.role != PaneRole.Single && from.key != to.key) {
        PaneChange.Replace
      } else {
        null
      }
    }
    else -> null
  }
}

/** No transition. The new screen is in place from the first frame, and moves with its pane. */
internal val DefaultPaneOpenTransform:
  AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform =
  {
    EnterTransition.None togetherWith ExitTransition.None
  }

/**
 * A crossfade. The old screen shrinks a little as it fades out, and the new screen grows to its
 * size with a small bounce as it fades in.
 */
internal val DefaultPaneReplaceTransform:
  AnimatedContentTransitionScope<AnimatedNavState>.() -> ContentTransform =
  {
    val enter =
      fadeIn(tween(durationMillis = 150, delayMillis = 50)) +
        scaleIn(
          animationSpec =
            spring(
              dampingRatio = Spring.DampingRatioLowBouncy,
              stiffness = Spring.StiffnessMediumLow,
            ),
          initialScale = 0.94f,
        )
    val exit = fadeOut(tween(durationMillis = 100)) + scaleOut(tween(100), targetScale = 0.97f)
    enter togetherWith exit
  }

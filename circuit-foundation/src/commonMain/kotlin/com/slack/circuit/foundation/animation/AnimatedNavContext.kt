// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.InternalCircuitNavigationApi
import com.slack.circuit.runtime.navigation.NavigationContext
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlinx.collections.immutable.ImmutableList

/**
 * Adds transitions to this [NavigationContext].
 *
 * @param block A lambda that configures an [AnimatedNavContext].
 * @return The modified [NavigationContext] with the [AnimatedNavContext] added as a tag.
 */
public fun NavigationContext.transition(
  block: AnimatedNavContext.Builder.() -> Unit
): NavigationContext {
  val builder = tag<AnimatedNavContext>()?.buildUpon() ?: AnimatedNavContext.Builder()
  putTag(builder.apply(block).build())
  return this
}

/**
 * Adds a transitions to a new [NavigationContext].
 *
 * @param block A lambda that configures an [AnimatedNavContext].
 * @return A new [NavigationContext] with the configured [AnimatedNavContext] as a tag.
 */
@OptIn(InternalCircuitNavigationApi::class)
public fun transition(block: AnimatedNavContext.Builder.() -> Unit): NavigationContext {
  return NavigationContext().apply { putTag(AnimatedNavContext.Builder().apply(block).build()) }
}

/**
 * Configures the 'forward' transition behavior for an [AnimatedNavContext].
 *
 * @param block A lambda that configures a [PartialContentTransform].
 */
public fun AnimatedNavContext.Builder.forward(block: PartialContentTransform.Builder.() -> Unit) {
  forward = PartialContentTransform.Builder().apply(block).build()
}

/**
 * Configures the 'reverse' transition behavior for an [AnimatedNavContext].
 *
 * @param block A lambda that configures a [PartialContentTransform].
 */
public fun AnimatedNavContext.Builder.reverse(block: PartialContentTransform.Builder.() -> Unit) {
  reverse = PartialContentTransform.Builder().apply(block).build()
}

/**
 * This is a convenience function that combines [Navigator.goTo] with a transition.
 *
 * @param screen The [Screen] to navigate to.
 * @param transition A lambda that configures an [AnimatedNavContext].
 * @return `true` if the navigation was successful, `false` otherwise.
 */
public fun Navigator.goTo(
  screen: Screen,
  transition: AnimatedNavContext.Builder.() -> Unit,
): Boolean {
  return goTo(screen, transition(transition))
}

/**
 * This is a convenience function that combines [Navigator.pop] with a transition.
 *
 * @param result The result to return to the previous screen, if any.
 * @param transform A lambda that configures a [PartialContentTransform].
 * @return The [Screen] that was popped.
 */
public fun Navigator.pop(
  result: PopResult? = null,
  transform: PartialContentTransform.Builder.() -> Unit,
): Screen? {
  return pop(result, transition { forward(transform) })
}

/**
 * This is a convenience function that combines [Navigator.resetRoot] with a transition.
 *
 * @param newRoot The new root [Screen] of the navigation stack.
 * @param saveState Whether to save the state of the current stack.
 * @param restoreState Whether to restore the state of the new stack.
 * @param transform A lambda that configures a [PartialContentTransform].
 * @return A list of screens representing the old navigation stack.
 */
public fun Navigator.resetRoot(
  newRoot: Screen,
  saveState: Boolean = false,
  restoreState: Boolean = false,
  transform: PartialContentTransform.Builder.() -> Unit,
): ImmutableList<Screen> {
  return resetRoot(newRoot = newRoot, saveState, restoreState, transition { forward(transform) })
}

/**
 * This class is used to customize how a [Screen] enters and exits the view during navigation
 * events.
 */
@Immutable
public class AnimatedNavContext
internal constructor(
  // todo Don't like the forward/backward names
  //    Forward is from the nav event, reverse only applies for a pop from a goTo
  public val forward: PartialContentTransform? = null,
  public val reverse: PartialContentTransform? = null,
) {

  /** Creates a new [Builder] instance, pre-populated with this [AnimatedNavContext] values. */
  public fun buildUpon(): Builder {
    return Builder().apply {
      forward = this@AnimatedNavContext.forward
      reverse = this@AnimatedNavContext.reverse
    }
  }

  /** Builder for creating an [AnimatedNavContext]. */
  @Stable
  public class Builder {
    /** The [PartialContentTransform] to use when navigating. */
    public var forward: PartialContentTransform? = null

    /** The [PartialContentTransform] to use when navigating in reverse. */
    public var reverse: PartialContentTransform? = null

    /** Builds an [AnimatedNavContext]. */
    public fun build(): AnimatedNavContext {
      return AnimatedNavContext(forward = forward, reverse = reverse)
    }
  }
}

/**
 * Represents a partial [ContentTransform] for animated navigation.
 *
 * This class allows you to customize specific parts of a [ContentTransform], such as the enter
 * transition, exit transition, z-index, and size transform.
 *
 * @property enter The [EnterTransition] to use when a screen enters the view.
 * @property exit The [ExitTransition] to use when a screen exits the view.
 * @property zIndex The z-index of the screen during the transition.
 * @property sizeTransform The [SizeTransform] to use during the transition.
 * @see ContentTransform
 */
public data class PartialContentTransform(
  val enter: EnterTransition?,
  val exit: ExitTransition?,
  val zIndex: Float?,
  val sizeTransform: SizeTransform?,
) {

  /** Builder for creating a [PartialContentTransform]. */
  @Stable
  public class Builder {
    /** The [EnterTransition] to use when a screen enters the view. */
    public var enter: EnterTransition? = null

    /** The [ExitTransition] to use when a screen exits the view. */
    public var exit: ExitTransition? = null

    /** The z-index of the screen during the transition. */
    public var zIndex: Float? = null

    /** The [SizeTransform] to use during the transition. */
    public var sizeTransform: SizeTransform? = null

    /** Builds a [PartialContentTransform] instance. */
    public fun build(): PartialContentTransform {
      return PartialContentTransform(enter, exit, zIndex, sizeTransform)
    }
  }

  public companion object {
    /** An empty [PartialContentTransform] with no transitions or transformations. */
    public val EMPTY: PartialContentTransform =
      PartialContentTransform(enter = null, exit = null, zIndex = null, sizeTransform = null)
  }
}

/** Converts a [ContentTransform] to a [PartialContentTransform]. */
public fun ContentTransform.asPartialContentTransform(): PartialContentTransform {
  return PartialContentTransform(
    enter = targetContentEnter,
    exit = initialContentExit,
    zIndex = targetContentZIndex,
    sizeTransform = sizeTransform,
  )
}

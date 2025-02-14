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

public fun NavigationContext.transition(
  block: AnimatedNavContext.Builder.() -> Unit
): NavigationContext {
  val builder = tag<AnimatedNavContext>()?.buildUpon() ?: AnimatedNavContext.Builder()
  putTag(builder.apply(block).build())
  return this
}

@OptIn(InternalCircuitNavigationApi::class)
public fun transition(block: AnimatedNavContext.Builder.() -> Unit): NavigationContext {
  return NavigationContext().apply { putTag(AnimatedNavContext.Builder().apply(block).build()) }
}

public fun AnimatedNavContext.Builder.forward(block: PartialContentTransform.Builder.() -> Unit) {
  forward = PartialContentTransform.Builder().apply(block).build()
}

public fun AnimatedNavContext.Builder.reverse(block: PartialContentTransform.Builder.() -> Unit) {
  reverse = PartialContentTransform.Builder().apply(block).build()
}

public fun Navigator.goTo(
  screen: Screen,
  transition: AnimatedNavContext.Builder.() -> Unit,
): Boolean {
  return goTo(screen, transition(transition))
}

public fun Navigator.pop(
  result: PopResult? = null,
  transform: PartialContentTransform.Builder.() -> Unit,
): Screen? {
  return pop(result, transition { forward(transform) })
}

public fun Navigator.resetRoot(
  newRoot: Screen,
  saveState: Boolean = false,
  restoreState: Boolean = false,
  transform: PartialContentTransform.Builder.() -> Unit,
): ImmutableList<Screen> {
  return resetRoot(newRoot = newRoot, saveState, restoreState, transition { forward(transform) })
}

@Immutable
public class AnimatedNavContext
internal constructor(
  // todo Don't like the forward/backward names
  //    Forward is from the nav event, reverse only applies for a the pop from a goTo
  public val forward: PartialContentTransform? = null,
  public val reverse: PartialContentTransform? = null,
) {

  public fun buildUpon(): Builder {
    return Builder().apply {
      forward = this@AnimatedNavContext.forward
      reverse = this@AnimatedNavContext.reverse
    }
  }

  @Stable
  public class Builder {
    // Forward A - goTo -> B
    // Backward B - pop -> A

    public var forward: PartialContentTransform? = null
    public var reverse: PartialContentTransform? = null

    public fun build(): AnimatedNavContext {
      return AnimatedNavContext(forward = forward, reverse = reverse)
    }
  }
}

public data class PartialContentTransform(
  val enter: EnterTransition?,
  val exit: ExitTransition?,
  val zIndex: Float?,
  val sizeTransform: SizeTransform?,
) {

  @Stable
  public class Builder {
    public var enter: EnterTransition? = null
    public var exit: ExitTransition? = null
    public var zIndex: Float? = null
    public var sizeTransform: SizeTransform? = null

    public fun build(): PartialContentTransform {
      return PartialContentTransform(enter, exit, zIndex, sizeTransform)
    }
  }

  public companion object {
    public val EMPTY: PartialContentTransform =
      PartialContentTransform(enter = null, exit = null, zIndex = null, sizeTransform = null)
  }
}

public fun ContentTransform.asPartialContentTransform(): PartialContentTransform {
  return PartialContentTransform(
    enter = targetContentEnter,
    exit = initialContentExit,
    zIndex = targetContentZIndex,
    sizeTransform = sizeTransform,
  )
}

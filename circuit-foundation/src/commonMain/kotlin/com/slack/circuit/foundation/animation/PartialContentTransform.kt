// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.foundation.animation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Stable

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

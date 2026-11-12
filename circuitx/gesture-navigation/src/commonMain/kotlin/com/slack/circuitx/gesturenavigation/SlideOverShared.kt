// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.EnterExitState.PreEnter
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.time.TimeSource.Monotonic

/** The three slide-over drag positions: settled center, or committed to the start/end edge. */
internal enum class SlideOverAnchor {
  Start,
  Center,
  End,
}

// 60Hz fallback frame interval until we've measured the real cadence.
private const val DefaultFrameIntervalNanos = 16_666_667L
// Reject implausibly short frame deltas (>~333Hz) as measurement glitches.
private const val MinPlausibleFrameNanos = 3_000_000L
// Fraction of a frame we're willing to spend precomposing on an idle frame.
private const val PreloadFrameFraction = 0.25

@Composable
internal fun <T : NavArgument> rememberNext(args: NavStackList<T>): NavStackList<T>? =
  remember(args) {
    if (args.forwardItems.any()) {
      val forward = args.forwardItems.drop(1)
      val currentItem = args.forwardItems.first()
      val backward = listOf(args.active) + args.backwardItems
      navStackListOf(forward, currentItem, backward)
    } else null
  }

@Composable
internal fun <T : NavArgument> rememberPrevious(args: NavStackList<T>): NavStackList<T>? =
  remember(args) {
    if (args.backwardItems.any()) {
      val forward = listOf(args.active) + args.forwardItems
      val currentItem = args.backwardItems.first()
      val backward = args.backwardItems.drop(1)
      navStackListOf(forward, currentItem, backward)
    } else null
  }

internal suspend fun <T : NavArgument> SeekableTransitionState<NavStackList<T>>.seekToNav(
  progress: Float,
  targetNavStack: NavStackList<T>?,
) {
  if (targetNavStack != null) {
    try {
      seekTo(
        fraction = abs(progress),
        targetState = targetNavStack,
      )
    } catch (_: CancellationException) {
      // If seekTo is interrupted we want to keep observing the swipeState
    }
  }
}

internal suspend fun <T : NavArgument> SeekableTransitionState<NavStackList<T>>.resetNav(
  args: NavStackList<T>
) {
  seekTo(1f - fraction, targetState = args)
  animateTo(targetState = args)
}

/**
 * Seeks toward [targetNavStack] while a gesture is in progress, and settles back to [args] on the
 * terminal `0f` frame so a cancelled swipe lands fully home instead of parking on a small fraction.
 */
internal suspend fun <T : NavArgument> SeekableTransitionState<NavStackList<T>>.seekOrReset(
  progress: Float,
  offset: Float,
  targetNavStack: NavStackList<T>?,
  args: NavStackList<T>,
) {
  if (offset.isNaN()) return
  if (progress != 0f) {
    seekToNav(progress, targetNavStack)
  } else if (targetState != args) {
    resetNav(args)
  }
}

/** A [AnimatedVisibilityScope] that takes a [Transition]. */
internal data class SimpleAnimatedVisibilityScope(
  override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope

// This converts Boolean visible to EnterExitState
@Composable
internal fun <T> Transition<T>.targetEnterExit(
  visible: (T) -> Boolean,
  targetState: T,
): EnterExitState =
  key(this) {
    if (this.isRunning) {
      if (visible(targetState)) {
        Visible
      } else {
        if (visible(this.currentState)) {
          PostExit
        } else {
          PreEnter
        }
      }
    } else {
      val hasBeenVisible = remember { mutableStateOf(false) }
      if (visible(currentState)) {
        hasBeenVisible.value = true
      }
      if (visible(targetState)) {
        Visible
      } else {
        // If never been visible, visible = false means PreEnter, otherwise PostExit
        if (hasBeenVisible.value) {
          PostExit
        } else {
          PreEnter
        }
      }
    }
  }

/**
 * Platform-agnostic per-frame pacing for off-screen precomposition.
 *
 * Self-calibrates the frame interval from [onFrameStart] timestamps (running min, so our own
 * slicing can't inflate it) and caps each frame's work at [PreloadFrameFraction]. Owns its own
 * intra-frame clock via [Monotonic], so there's no cross-timebase comparison against the frame
 * timestamp.
 */
internal class FractionFrameBudget(private val fraction: Double = PreloadFrameFraction) {
  init {
    require(fraction in 0.0..1.0) { "fraction must be in 0.0..1.0, was $fraction" }
  }

  private var frameIntervalNanos = DefaultFrameIntervalNanos
  private var previousFrameTimeNanos = -1L
  private var budgetNanos = (DefaultFrameIntervalNanos * fraction).toLong()
  private var sliceStart = Monotonic.markNow()

  /** Call once at the start of each frame with `withFrameNanos`'s value. */
  fun onFrameStart(frameTimeNanos: Long) {
    if (previousFrameTimeNanos >= 0L) {
      val delta = frameTimeNanos - previousFrameTimeNanos
      if (delta >= MinPlausibleFrameNanos) frameIntervalNanos = minOf(frameIntervalNanos, delta)
    }
    previousFrameTimeNanos = frameTimeNanos
    budgetNanos = (frameIntervalNanos * fraction).toLong()
    sliceStart = Monotonic.markNow()
  }

  /** True once we've spent our slice for this frame and should stop resuming. */
  fun isExhausted(): Boolean = sliceStart.elapsedNow().inWholeNanoseconds >= budgetNanos
}

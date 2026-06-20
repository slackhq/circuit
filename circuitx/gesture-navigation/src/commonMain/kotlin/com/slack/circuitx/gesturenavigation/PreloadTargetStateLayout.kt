package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.runtime.navigation.NavArgument
import kotlinx.coroutines.awaitCancellation
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic

// 60Hz fallback frame interval until we've measured the real cadence.
private const val DefaultFrameIntervalNanos = 16_666_667L
// Reject implausibly short frame deltas (>~333Hz) as measurement glitches.
private const val MinPlausibleFrameNanos = 3_000_000L
// Fraction of a frame we're willing to spend precomposing on an idle frame.
private const val PreloadFrameFraction = 0.5


/**
 * Eagerly precomposes the next forward screen so it's ready before a gesture starts.
 *
 * Uses [SubcomposeLayoutState.createPausedPrecomposition] to compose the next screen off-screen
 * while the current screen is idle. Once composition completes, the precomposition is applied and
 * the presenter is paused until actually navigated to. This eliminates the composition cost during
 * the first frames of a forward swipe gesture, keeping it smooth.
 *
 * Cancels any in-flight precomposition immediately when [targetState] changes so stale screens are
 * never retained.
 */
@Suppress("SlotReused") // innerContent slot is reused across the precompose + subcompose paths
@Composable
internal fun <T : NavArgument> PreloadTargetStateLayout(
  targetState: T?,
  shouldPreload: Boolean,
  innerContent: @Composable ((T) -> Unit),
) {
  if (!shouldPreload || targetState == null) return

  // Everything is keyed on targetState, so when it changes these are re-created and the effect
  // below restarts, running its `finally` cleanup. No manual "cancel on change" bookkeeping.
  val subcomposeLayoutState = remember(targetState) { SubcomposeLayoutState() }
  // Flips true once the precomposition is applied and the slot is ready to place. Must be
  // snapshot state so the SubcomposeLayout below re-measures when composition finishes.
  var isApplied by remember(targetState) { mutableStateOf(false) }
  // Keep the presenter active while composing, then pause it once applied. Must be snapshot
  // state so flipping it actually recomposes ProvideRecordLifecycle.
  var isActive by remember(targetState) { mutableStateOf(true) }

  val composer = currentComposer
  // Same content reference for both the precompose and the subcompose-by-slotId path, so the
  // warmed slot is reused rather than recomposed, and both see one isActive source of truth.
  val preloadContent: @Composable () -> Unit = {
    ProvideRecordLifecycle(isActive = isActive) { innerContent(targetState) }
  }

  LaunchedEffect(subcomposeLayoutState) {
    val pausedComposition =
      subcomposeLayoutState.createPausedPrecomposition(targetState.key, preloadContent)
    var handle: SubcomposeLayoutState.PrecomposedSlotHandle? = null
    val budget = FractionFrameBudget()
    try {
      // Spread composition across frames, resuming a budget-sized slice per frame. shouldPause
      // reads current system state: yield if the host has pending recompositions (don't compete)
      // or if we've spent our slice of this frame.
      while (!pausedComposition.isComplete) {
        withFrameNanos { budget.onFrameStart(it) }
        pausedComposition.resume {
          composer.composition.let { it.hasPendingChanges || it.isComposing } || budget.isExhausted()
        }
      }
      handle = pausedComposition.apply()
      isActive = false // composed — pause the presenter
      isApplied = true // trigger the SubcomposeLayout below to place the warmed slot
      awaitCancellation()
    } finally {
      // Applied -> dispose the handle (no-op if subcompose already consumed the slot).
      // Not applied -> cancel the still-incomplete precomposition.
      handle?.dispose() ?: pausedComposition.cancel()
    }
  }

  // Placing the warmed slot drives platform UI caching (e.g. interop fragment attach).
  SubcomposeLayout(subcomposeLayoutState) { constraints ->
    if (isApplied) {
      val placeable = subcompose(targetState.key, preloadContent).first().measure(constraints)
      val size = constraints.constrain(IntSize(placeable.width, placeable.height))
      layout(size.width, size.height) { placeable.place(size.width, 0) }
    } else {
      layout(0, 0) {}
    }
  }
}

/**
 * Platform-agnostic per-frame pacing for off-screen precomposition.
 *
 * Self-calibrates the frame interval from [onFrameStart] timestamps (running min, so our own
 * slicing can't inflate it) and caps each frame's work at [PreloadFrameFraction]. Owns its own
 * intra-frame clock via [TimeSource.Monotonic], so there's no cross-timebase comparison against
 * the frame timestamp.
 */
private class FractionFrameBudget(private val fraction: Double = PreloadFrameFraction) {
  private var frameIntervalNanos = DefaultFrameIntervalNanos
  private var previousFrameTimeNanos = -1L
  private var budgetNanos = (DefaultFrameIntervalNanos * fraction).toLong()
  private var sliceStart = TimeSource.Monotonic.markNow()

  /** Call once at the start of each frame with `withFrameNanos`'s value. */
  fun onFrameStart(frameTimeNanos: Long) {
    if (previousFrameTimeNanos >= 0L) {
      val delta = frameTimeNanos - previousFrameTimeNanos
      if (delta >= MinPlausibleFrameNanos) frameIntervalNanos = minOf(frameIntervalNanos, delta)
    }
    previousFrameTimeNanos = frameTimeNanos
    budgetNanos = (frameIntervalNanos * fraction).toLong()
    sliceStart = TimeSource.Monotonic.markNow()
  }

  /** True once we've spent our slice for this frame and should stop resuming. */
  fun isExhausted(): Boolean = sliceStart.elapsedNow().inWholeNanoseconds >= budgetNanos
}

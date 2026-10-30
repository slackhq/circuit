// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeLayoutState.PrecomposedSlotHandle
import com.slack.circuit.runtime.navigation.NavArgument
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first

/**
 * Precomposes [target] into [layoutState] across idle frames (paced by [FractionFrameBudget]), so
 * the shared record [androidx.compose.ui.layout.SubcomposeLayout] can adopt the warmed node when
 * navigation reaches it instead of composing it cold on the first slide frame. [onReady] fires with
 * `true` once the precomposition is applied and adoptable, `false` when it is torn down. A [target]
 * that is already a live slot warms nothing and reports nothing, since it is hosted already.
 *
 * The precomposition shares [layoutState] with the on-screen slots, so `subcompose(target.key)`
 * reuses this exact node (`precomposeMap.remove`) — one node, one `_registry_` registration.
 * [warmContent] must be the same instance the on-screen placement uses (the record's shared
 * `RecordHost.slotContent`) so adoption reuses the composition rather than discarding it as
 * changed.
 *
 * @param enabled whether a swipe toward [target] is possible; controls presence. Stays true across
 *   a swipe (nav args commit only on settle), so the warmed node survives to be adopted.
 * @param settledProvider reads whether the current screen is idle. Warming only spends a frame
 *   while this is true, so it never competes with an in-progress transition; a gesture parks it
 *   part-composed until the screen settles again. The effect is NOT keyed on this: a swipe must not
 *   cancel an applied precomposition, or the record would leave and re-enter the subcompose set and
 *   re-register its `_registry_` key.
 */
@Composable
internal fun <T : NavArgument> PreloadAdjacentRecord(
  target: T?,
  enabled: Boolean,
  settledProvider: () -> Boolean,
  layoutState: SubcomposeLayoutState,
  warmContent: (@Composable () -> Unit)?,
  onReady: (target: T, ready: Boolean) -> Unit,
  onFailed: (target: T, cause: Throwable) -> Unit,
) {
  if (!enabled || target == null || warmContent == null) return

  val currentOnReady = rememberUpdatedState(onReady)
  val currentOnFailed = rememberUpdatedState(onFailed)
  val composer = currentComposer

  // Keyed on target only so a new adjacent cancels the in-flight precomposition and starts fresh; a
  // settle/unsettle flip must not, so the warmed node is never disposed and re-created mid-swipe.
  LaunchedEffect(target) {
    val settledFlow = snapshotFlow(settledProvider)
    settledFlow.first { it }
    // Precompose the SAME lambda instance the on-screen warm slot uses, so adoption reuses this
    // composition (contentChanged == false) instead of re-running it.
    val pausedComposition = layoutState.createPausedPrecomposition(target.key, warmContent)
    // SubcomposeLayout skips precompose for a key that is already a live slot, and the handle then
    // reports complete without having composed anything. Nothing to warm, nothing to report.
    if (pausedComposition.isComplete) {
      pausedComposition.cancel()
      return@LaunchedEffect
    }
    val budget = FractionFrameBudget()
    // Non-null once applied. Frees the node only while it is still precomposed: adoption mid-pace
    // hands ownership to SubcomposeLayout, making dispose() below a no-op.
    var handle: PrecomposedSlotHandle? = null
    try {
      // Spread composition across frames, resuming a budget-sized slice per frame. Yield if the
      // host has pending recompositions (don't compete) or once the frame slice is spent.
      while (!pausedComposition.isComplete) {
        withFrameNanos { budget.onFrameStart(it) }
        // Read after the frame starts, not before: a gesture that began since the last slice would
        // otherwise get one more.
        if (!settledProvider()) {
          settledFlow.first { it }
          continue
        }
        pausedComposition.resume {
          composer.composition.let { it.hasPendingChanges || it.isComposing } ||
            budget.isExhausted()
        }
      }
      handle = pausedComposition.apply()
      currentOnReady.value(target, true)
      awaitCancellation()
    } catch (e: CancellationException) {
      throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      // Best-effort warm-up must never crash the visible screen, but a failure here usually means
      // the `_registry_` invariant regressed on the warm path, so report it to the caller.
      currentOnFailed.value(target, e)
    } finally {
      currentOnReady.value(target, false)
      // Free the precomposed slot on teardown. Post-apply, dispose() removes it from the shared
      // precomposeMap (a no-op if the measure pass already adopted it); pre-apply, cancel() drops
      // the still-pending composition. Without this an applied-but-never-adopted node leaks.
      val appliedHandle = handle
      if (appliedHandle != null) appliedHandle.dispose() else pausedComposition.cancel()
    }
  }
}

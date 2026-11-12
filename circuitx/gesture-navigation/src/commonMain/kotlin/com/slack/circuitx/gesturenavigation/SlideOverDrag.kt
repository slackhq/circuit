// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

// Below this fling velocity (px/s) we settle by position alone; above it the sign of the velocity
// picks the target anchor. Matches the foundation default (AnchoredDraggableMinFlingVelocity).
private val MinFlingVelocity: Dp = 125.dp
// Fraction of the screen the drag must cross to commit when released below the fling velocity.
private const val PositionalThreshold = 0.5f
// Half a pixel from the anchor is close enough to end the settle.
private const val SettleThresholdPx = 0.5f
// Settle spec for a release the fling can't decay all the way to its anchor. A spring starts from
// the release velocity, where the foundation default (a tween) drops it and ramps up from a
// standstill.
private val SettleSpec =
  spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = SettleThresholdPx,
  )

/** Drag events passed from the pointer-input loop to the coroutine holding the drag mutex. */
private sealed interface DragSignal {
  /** A drag delta in px to apply to the offset. */
  @JvmInline value class Delta(val amount: Float) : DragSignal

  /** The finger lifted; carries the terminal velocity used to pick the settle target. */
  @JvmInline value class Stop(val velocity: Float) : DragSignal
}

private fun AnchoredDraggableState<SlideOverAnchor>.offsetOrZero(): Float =
  if (offset.isNaN()) 0f else offset

/**
 * Drives [state] like `Modifier.anchoredDraggable`, but only claims a swipe whose direction has an
 * anchor to move toward. `anchoredDraggable` consumes every horizontal drag once enabled, so a
 * swipe toward a missing anchor (e.g. rightward at a tab root with no [SlideOverAnchor.End]) is
 * swallowed and never reaches the outer `ModalNavigationDrawer`; leaving it unconsumed lets it
 * propagate to a parent, such as a navigation drawer.
 *
 * @param settleScope scope for the settle after lift; the pointer-input coroutine ends at gesture
 *   end so settling can't run on it.
 */
internal fun Modifier.directionalAnchoredDrag(
  state: AnchoredDraggableState<SlideOverAnchor>,
  enabled: Boolean,
  settleScope: CoroutineScope,
): Modifier {
  if (!enabled) return this
  return pointerInput(state) {
    // Clamp fling velocity to the platform max, matching androidx DragGestureNode.sendDragStopped.
    val maxFlingVelocity = viewConfiguration.maximumFlingVelocity
    val minFlingVelocityPx = MinFlingVelocity.toPx()
    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = false)
      // Feed full PointerInputChanges (with sub-frame history) to the tracker, not single points,
      // so fling velocity matches what the real modifier computes.
      val velocityTracker = VelocityTracker()
      velocityTracker.addPointerInputChange(down)
      // Only claim the drag if its direction has a reachable anchor. Returning without consuming
      // leaves the event for the parent.
      val claimed = awaitDirectionalSlop(down.id, state) ?: return@awaitEachGesture
      velocityTracker.addPointerInputChange(claimed)

      // Bridge the pointer-input loop to a coroutine that owns the AnchoredDraggableState drag
      // mutex for the whole gesture, mirroring androidx DragGestureNode. Holding the mutex means a
      // later gesture (or a programmatic animateTo) cancels this drag and any in-flight settle
      // cleanly via the shared mutex, instead of dispatchRawDelta racing the offset. The terminal
      // velocity rides through the channel as a Stop event (like DragGestureNode's DragStopped)
      // rather than a shared var, so there's no cross-thread read after close.
      val events = Channel<DragSignal>(Channel.UNLIMITED)
      settleScope.launch {
        var endVelocity = 0f
        state.anchoredDrag {
          for (event in events) {
            when (event) {
              is DragSignal.Delta -> {
                val next =
                  (state.offsetOrZero() + event.amount).coerceIn(
                    state.anchors.minPosition(),
                    state.anchors.maxPosition(),
                  )
                dragTo(next)
              }
              is DragSignal.Stop -> {
                endVelocity = event.velocity
                break
              }
            }
          }
        }
        // Finger lifted: settle to the resolved anchor, carrying the release velocity so a hard
        // flick decays into the anchor instead of restarting from rest. The settle re-acquires the
        // same mutex, so a new gesture that arrives mid-settle interrupts it rather than fighting
        // it.
        state.animateToWithDecay(
          targetValue = state.settleTarget(endVelocity, minFlingVelocityPx),
          velocity = endVelocity,
          snapAnimationSpec = SettleSpec,
        )
      }

      try {
        val completed =
          horizontalDrag(claimed.id) { change ->
            events.trySend(DragSignal.Delta(change.positionChange().x))
            velocityTracker.addPointerInputChange(change)
            change.consume()
          }
        val endVelocity =
          if (completed) {
            velocityTracker.calculateVelocity(Velocity(maxFlingVelocity, maxFlingVelocity)).x
          } else {
            0f
          }
        events.trySend(DragSignal.Stop(endVelocity))
      } finally {
        events.close()
      }
    }
  }
}

/**
 * Waits for a horizontal-dominant drag to cross the touch slop and claims it only if its direction
 * has an anchor: leftward needs [SlideOverAnchor.Start], rightward needs [SlideOverAnchor.End].
 *
 * Whichever axis crosses the slop first decides. A vertical (or diagonal) swipe crosses vertically
 * first, so we bail with null and never consume, leaving the gesture for the parent. We only claim
 * when horizontal crosses while vertical is still under the slop, so a mostly-vertical drag with a
 * little horizontal drift no longer triggers the slide-over.
 */
private suspend fun AwaitPointerEventScope.awaitDirectionalSlop(
  pointerId: PointerId,
  state: AnchoredDraggableState<SlideOverAnchor>,
): PointerInputChange? {
  val touchSlop = viewConfiguration.touchSlop
  var totalDx = 0f
  var totalDy = 0f
  var claim: PointerInputChange? = null
  var resolved = false
  while (!resolved) {
    val event = awaitPointerEvent()
    val change = event.changes.firstOrNull { it.id == pointerId }
    if (change == null || change.isConsumed || change.changedToUpIgnoreConsumed()) {
      resolved = true
    } else {
      totalDx += change.positionChange().x
      totalDy += change.positionChange().y
      // Vertical crossed first (or a diagonal crossed both at once): not our gesture, so stop.
      if (abs(totalDy) > touchSlop) {
        resolved = true
      } else if (abs(totalDx) > touchSlop) {
        val wanted = if (totalDx < 0f) SlideOverAnchor.Start else SlideOverAnchor.End
        // Claim only when the direction has an anchor; otherwise leave it for the parent.
        if (state.anchors.hasPositionFor(wanted)) {
          change.consume()
          claim = change
        }
        resolved = true
      }
    }
  }
  return claim
}

/**
 * Picks the settle anchor after lift: above [minFlingVelocityPx] the velocity sign wins, otherwise
 * commit to the edge only if the offset crossed [PositionalThreshold] of the width, else snap back
 * to [SlideOverAnchor.Center]. Anchors that don't exist fall back to Center.
 */
private fun AnchoredDraggableState<SlideOverAnchor>.settleTarget(
  velocityX: Float,
  minFlingVelocityPx: Float,
): SlideOverAnchor {
  val currentOffset = offsetOrZero()
  // The edge anchor the current offset is heading toward: negative offset -> Start, positive ->
  // End.
  val directionalAnchor = if (currentOffset < 0f) SlideOverAnchor.Start else SlideOverAnchor.End
  val candidate =
    when {
      velocityX <= -minFlingVelocityPx -> SlideOverAnchor.Start
      velocityX >= minFlingVelocityPx -> SlideOverAnchor.End
      else -> {
        val anchorPosition = anchors.positionOf(directionalAnchor)
        val crossedThreshold =
          !anchorPosition.isNaN() && abs(currentOffset) >= abs(anchorPosition) * PositionalThreshold
        if (crossedThreshold) directionalAnchor else SlideOverAnchor.Center
      }
    }
  return if (anchors.hasPositionFor(candidate)) candidate else SlideOverAnchor.Center
}

internal fun AnchoredDraggableState<SlideOverAnchor>.updateAnchors(
  intSize: IntSize,
  canGoForward: Boolean,
  canGoBackward: Boolean,
) {
  val width = intSize.width.toFloat()
  updateAnchors(
    newAnchors =
      DraggableAnchors {
        SlideOverAnchor.Center at 0f
        if (canGoBackward) {
          SlideOverAnchor.End at width
        }
        if (canGoForward) {
          SlideOverAnchor.Start at -width
        }
      },
    newTarget = SlideOverAnchor.Center,
  )
}

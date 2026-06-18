// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Default threshold for completing an edge gesture (0.5 = 50% of screen width). */
private const val DefaultCompletionThreshold = 0.5f

private val BackwardCompletionDistance = 125.dp
private val ForwardCompletionDistance = 200.dp

/** Direction of the gesture-driven navigation. */
internal enum class GestureDirection {
  /** Backward navigation - sliding content to the right to reveal the previous screen. */
  Backward,
  /** Forward navigation - sliding content to the left as the next screen covers it. */
  Forward,
}

/**
 * A modifier that detects horizontal swipes (forward + backward) AND turns horizontal overscroll
 * from child scrollables into the same gesture. Both halves share `claimedDirection` and
 * `totalDragX` inside a single [Node], so a gesture that starts in pointer input and
 * continues via nested-scroll (or vice versa) accumulates against one source of truth instead of
 * drifting between two separate counters.
 *
 * After exceeding the touch slop, the sign of the drag determines which direction the gesture is
 * for — leftward (negative) -> [GestureDirection.Forward], rightward (positive) ->
 * [GestureDirection.Backward]. The gesture is only claimed if travel is in a direction that is
 * currently enabled.
 *
 * Progress is reported as a value from 0-1 based on how far the user has dragged in the active
 * direction.
 *
 * @param canGoBackward Whether backward navigation is available (rightward swipes/overscroll)
 * @param canGoForward Whether forward navigation is available (leftward swipes/overscroll)
 * @param onProgress Callback with the active direction and drag progress (0-1)
 * @param onCompleted Callback with the direction when the gesture passes the threshold
 * @param onCancelled Callback when an engaged gesture is cancelled or doesn't reach threshold
 * @param backwardCompletionDistance Progress distance to complete navigation (default 200.dp)
 * @param forwardCompletionDistance Progress distance to complete navigation (default 200.dp)
 */
@Suppress("LongParameterList")
internal fun Modifier.slideOverGesture(
  canGoBackward: Boolean,
  canGoForward: Boolean,
  onProgress: (GestureDirection, progress: Float) -> Unit,
  onCompleted: (GestureDirection) -> Unit,
  onCancelled: (GestureDirection) -> Unit,
  backwardCompletionDistance: Dp = BackwardCompletionDistance,
  forwardCompletionDistance: Dp = ForwardCompletionDistance,
): Modifier {
  if (!canGoBackward && !canGoForward) return this
  return this then
    SlideOverGestureElement(
      canGoBackward = canGoBackward,
      canGoForward = canGoForward,
      backwardCompletionDistance = backwardCompletionDistance,
      forwardCompletionDistance = forwardCompletionDistance,
      onProgress = onProgress,
      onCompleted = onCompleted,
      onCancelled = onCancelled,
    )
}

@Suppress("LongParameterList")
private class SlideOverGestureElement(
  private val canGoBackward: Boolean,
  private val canGoForward: Boolean,
  private val backwardCompletionDistance: Dp,
  private val forwardCompletionDistance: Dp,
  private val onProgress: (GestureDirection, progress: Float) -> Unit,
  private val onCompleted: (GestureDirection) -> Unit,
  private val onCancelled: (GestureDirection) -> Unit,
) : ModifierNodeElement<SlideOverGestureNode>() {
  override fun create(): SlideOverGestureNode =
    SlideOverGestureNode(
      canGoBackward = canGoBackward,
      canGoForward = canGoForward,
      backwardCompletionDistance = backwardCompletionDistance,
      forwardCompletionDistance = forwardCompletionDistance,
      onProgress = onProgress,
      onCompleted = onCompleted,
      onCancelled = onCancelled,
    )

  override fun update(node: SlideOverGestureNode) {
    node.update(
      canGoBackward = canGoBackward,
      canGoForward = canGoForward,
      backwardCompletionDistance = backwardCompletionDistance,
      forwardCompletionDistance = forwardCompletionDistance,
      onProgress = onProgress,
      onCompleted = onCompleted,
      onCancelled = onCancelled,
    )
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "slideOverGesture"
    properties["canGoBackward"] = canGoBackward
    properties["canGoForward"] = canGoForward
    properties["backwardCompletionDistance"] = backwardCompletionDistance
    properties["forwardCompletionDistance"] = forwardCompletionDistance
  }

  // Exclude callback lambdas from equals/hashCode. Decoration() recreates them every recomposition,
  // and during gesture-driven seekTo a data-class equals would always be false, firing update() per
  // frame. The lambdas' captures (swipeState map, seekableTransitionState, scope) are stable
  // instances, so keeping the original references is functionally equivalent.
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SlideOverGestureElement) return false
    return canGoBackward == other.canGoBackward &&
      canGoForward == other.canGoForward &&
      backwardCompletionDistance == other.backwardCompletionDistance &&
      forwardCompletionDistance == other.forwardCompletionDistance
  }

  override fun hashCode(): Int {
    var result = canGoBackward.hashCode()
    result = 31 * result + canGoForward.hashCode()
    result = 31 * result + backwardCompletionDistance.hashCode()
    result = 31 * result + forwardCompletionDistance.hashCode()
    return result
  }
}

/**
 * Single shared node that owns both the pointer-input gesture detector and the
 * [NestedScrollConnection]. Width comes from [onRemeasured], decay/touch-slop from composition
 * locals at the node level — no composition state is required at the call site.
 *
 * `claimedDirection` and `totalDragX` are shared fields rather than per-path locals: a swipe that
 * begins as a direct pointer drag can later receive nested-scroll deltas (or vice versa) and the
 * accumulator stays consistent across the handoff.
 */
@Suppress("LongParameterList")
private class SlideOverGestureNode(
  private var canGoBackward: Boolean,
  private var canGoForward: Boolean,
  private var backwardCompletionDistance: Dp,
  private var forwardCompletionDistance: Dp,
  private var onProgress: (GestureDirection, progress: Float) -> Unit,
  private var onCompleted: (GestureDirection) -> Unit,
  private var onCancelled: (GestureDirection) -> Unit,
) : DelegatingNode(), LayoutAwareModifierNode, NestedScrollConnection {

  private var width: Float = 0f
  private var backwardCompletionThreshold: Float = DefaultCompletionThreshold
  private var forwardCompletionThreshold: Float = DefaultCompletionThreshold

  // Pointer-input side state.
  private val velocityTracker = VelocityTracker()
  private val decay: DecayAnimationSpec<Float> = exponentialDecay()

  // Shared between pointer-input and nested-scroll so a gesture that hands off between paths keeps
  // a single accumulator.
  private var claimedDirection: GestureDirection? = null
  private var totalDragX: Float = 0f
  // Once we cancel mid-gesture, poison the rest of the gesture so we don't re-claim until the user
  // lifts. Cleared at every gesture boundary (pointer-input gesture start/end, onPreFling).
  private var poisoned: Boolean = false

  init {
    delegate(SuspendingPointerInputModifierNode { detectSlideOverDrag() })
    delegate(nestedScrollModifierNode(connection = this, dispatcher = null))
  }

  @Suppress("LongParameterList")
  fun update(
    canGoBackward: Boolean,
    canGoForward: Boolean,
    backwardCompletionDistance: Dp,
    forwardCompletionDistance: Dp,
    onProgress: (GestureDirection, progress: Float) -> Unit,
    onCompleted: (GestureDirection) -> Unit,
    onCancelled: (GestureDirection) -> Unit,
  ) {
    val completionChanged =
      this.backwardCompletionDistance != backwardCompletionDistance ||
        this.forwardCompletionDistance != forwardCompletionDistance

    this.canGoBackward = canGoBackward
    this.canGoForward = canGoForward
    this.backwardCompletionDistance = backwardCompletionDistance
    this.forwardCompletionDistance = forwardCompletionDistance
    this.onProgress = onProgress
    this.onCompleted = onCompleted
    this.onCancelled = onCancelled

    if (completionChanged) {
      calculateThresholds()
    }
  }

  override fun onAttach() {
    super.onAttach()
    calculateThresholds()
  }

  override fun onRemeasured(size: IntSize) {
    width = size.width.toFloat()
    calculateThresholds()
  }

  private fun calculateThresholds() {
    if (!isAttached || width <= 0) return
    with(requireDensity()) {
      backwardCompletionThreshold = (backwardCompletionDistance.toPx() / width).coerceIn(0f, 1f)
      forwardCompletionThreshold = (forwardCompletionDistance.toPx() / width).coerceIn(0f, 1f)
    }
  }

  // ---------- Pointer input ----------

  private suspend fun PointerInputScope.detectSlideOverDrag() {
    awaitEachGesture {
      if (!canGoBackward && !canGoForward) return@awaitEachGesture
      // New finger down clears any poison left from a prior cancelled nested-scroll gesture.
      poisoned = false
      val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
      val claim = awaitDirectionalSlop(down.id) ?: return@awaitEachGesture
      val gestureWidth = width
      velocityTracker.resetTracking()

      claimedDirection = claim.direction
      totalDragX = claim.initialOffset.clampToDirection(claim.direction, gestureWidth)
      fun reportProgress() {
        onProgress(claim.direction, computeProgress(totalDragX, gestureWidth))
      }
      try {
        reportProgress()

        val dragSuccess =
          horizontalDrag(claim.dragPointerId) { change ->
            val delta = change.positionChange().x
            totalDragX = (totalDragX + delta).clampToDirection(claim.direction, gestureWidth)
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            reportProgress()
          }

        if (dragSuccess) {
          val velocityX = velocityTracker.calculateVelocity().x
          val finalProgress = calculateFinalProgress(velocityX, totalDragX)
          if (willSettlePastThreshold(direction = claim.direction, finalProgress = finalProgress)) {
            // Continue to animate the fling
            coroutineScope.launch {
              Animatable(totalDragX).animateDecay(
                initialVelocity = velocityX * totalDragX.sign,
                animationSpec = exponentialDecay(),
              ) {
                totalDragX = (totalDragX + value).clampToDirection(claim.direction, gestureWidth)
                reportProgress()
              }
              onCompleted(claim.direction)
              resetSwipe()
            }
            return@awaitEachGesture
          }
        }
        // Drag didn't succeed or pass the threshold so cancel it.
        onCancelled(claim.direction)
        resetSwipe()
      } catch (e: CancellationException) {
        onCancelled(claim.direction)
        resetSwipe()
        throw e
      }
    }
  }

  private suspend fun AwaitPointerEventScope.awaitDirectionalSlop(
    downId: PointerId
  ): DirectionalClaim? {
    var claimedDirection: GestureDirection? = null
    var initialOffset = 0f
    val drag: PointerInputChange? =
      awaitHorizontalTouchSlopOrCancellation(downId) { change, overSlop ->
        val candidate = chooseDirection(overSlop, canGoBackward, canGoForward)
        if (candidate != null && !change.isConsumed) {
          claimedDirection = candidate
          initialOffset = overSlop
          change.consume()
        }
      }
    val direction = claimedDirection
    return if (drag != null && direction != null) {
      DirectionalClaim(direction, drag.id, initialOffset)
    } else {
      null
    }
  }

  private fun calculateFinalProgress(velocityX: Float, totalDragX: Float): Float {
    if (width <= 0f) return 0f
    val targetOffsetX = decay.calculateTargetValue(totalDragX, velocityX)
    return computeProgress(targetOffsetX, width)
  }

  private fun willSettlePastThreshold(direction: GestureDirection, finalProgress: Float): Boolean {
    if (width <= 0f) return false
    return when (direction) {
      GestureDirection.Forward -> finalProgress < -forwardCompletionThreshold
      GestureDirection.Backward -> finalProgress > backwardCompletionThreshold
    }
  }

  // ---------- Nested scroll ----------

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    if (!shouldHandleNestedScroll(source, available.x)) return Offset.Zero
    val claimed = claimedDirection ?: return Offset.Zero
    val incoming = available.x
    // Only intercept opposing motion (scroll that unwinds totalDragX toward zero). Same-direction
    // scroll passes to the child, which will overscroll and surface in onPostScroll if it can't
    // consume.
    val isOpposing =
      when (claimed) {
        GestureDirection.Forward -> incoming > 0f
        GestureDirection.Backward -> incoming < 0f
      }
    if (!isOpposing) return Offset.Zero
    return consumeClaimedScroll(claimed, incoming)
  }

  override fun onPostScroll(
    consumed: Offset,
    available: Offset,
    source: NestedScrollSource,
  ): Offset {
    if (!shouldHandleNestedScroll(source, available.x)) return Offset.Zero
    val leftover = available.x
    val claimed = claimedDirection
    return if (claimed != null) {
      consumeClaimedScroll(claimed, leftover)
    } else {
      claimNewScroll(leftover)
    }
  }

  private fun shouldHandleNestedScroll(source: NestedScrollSource, leftover: Float): Boolean {
    if (source != NestedScrollSource.UserInput || poisoned) return false
    return leftover != 0f && width > 0f
  }

  private fun consumeClaimedScroll(claimed: GestureDirection, leftover: Float): Offset {
    // Consume up to the amount needed to bring totalDragX back to 0 in the claimed direction.
    // Any leftover beyond that point cancels the gesture and propagates to the child.
    val nextDrag = (totalDragX + leftover).clampToDirection(claimed, width)
    val consumedX = nextDrag - totalDragX
    totalDragX = nextDrag
    onProgress(claimed, computeProgress(totalDragX, width))
    if (totalDragX == 0f) {
      onCancelled(claimed)
      resetSwipe()
      poisoned = true
    }
    return Offset(x = consumedX, y = 0f)
  }

  private fun claimNewScroll(leftover: Float): Offset {
    val direction =
      chooseDirection(leftover, canGoBackward, canGoForward)?.also { claimedDirection = it }
        ?: return Offset.Zero
    val prevDrag = totalDragX
    totalDragX = (totalDragX + leftover).clampToDirection(direction, width)
    val consumedX = totalDragX - prevDrag
    onProgress(direction, computeProgress(totalDragX, width))
    return Offset(x = consumedX, y = 0f)
  }

  override suspend fun onPreFling(available: Velocity): Velocity {
    val direction = claimedDirection
    // Always clear poison at the gesture boundary so the next gesture can claim again.
    poisoned = false
    if (direction == null) return Velocity.Zero
    val finalProgress = calculateFinalProgress(available.x, totalDragX)
    val completed = willSettlePastThreshold(direction = direction, finalProgress = finalProgress)
    if (completed) {
      onCompleted(direction)
    } else {
      onCancelled(direction)
    }
    resetSwipe()
    // Consume the horizontal velocity either way: we either committed nav, or we cancelled and
    // don't want the child running its own edge fling for a gesture we already claimed.
    return Velocity(x = available.x, y = 0f)
  }

  private fun resetSwipe() {
    claimedDirection = null
    totalDragX = 0f
    poisoned = false
  }
}

/** Result of a successful directional touch-slop claim. */
private data class DirectionalClaim(
  val direction: GestureDirection,
  val dragPointerId: PointerId,
  val initialOffset: Float,
)

private fun chooseDirection(
  overSlop: Float,
  canGoBackward: Boolean,
  canGoForward: Boolean,
): GestureDirection? =
  when {
    overSlop < 0 && canGoForward -> GestureDirection.Forward
    overSlop > 0 && canGoBackward -> GestureDirection.Backward
    else -> null
  }

private fun computeProgress(x: Float, width: Float): Float =
  if (width > 0f) (x / width).coerceIn(-1f, 1f) else 0f

/** Clamp [this] drag distance to the half-axis that matches [direction]. */
private fun Float.clampToDirection(direction: GestureDirection, width: Float): Float =
  when (direction) {
    GestureDirection.Forward -> coerceIn(-width, 0f)
    GestureDirection.Backward -> coerceIn(0f, width)
  }

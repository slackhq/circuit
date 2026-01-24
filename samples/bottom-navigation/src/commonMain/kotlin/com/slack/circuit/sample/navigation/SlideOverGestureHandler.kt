// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A modifier that detects horizontal swipes for forward navigation.
 *
 * This modifier uses pointer input to detect horizontal drags that are not consumed by child
 * content. When a leftward drag is detected and children haven't handled it, the gesture is claimed
 * for forward navigation.
 *
 * Progress is reported as a value from 0-1 based on how far left the user has dragged.
 *
 * @param enabled Whether forward navigation is available
 * @param completionThreshold Progress threshold to complete navigation (default 0.5f)
 * @param onProgress Callback with (progress: Float, offset: Offset) during drag
 * @param onCompleted Callback when drag completes past threshold
 * @param onCancelled Callback when drag is cancelled or doesn't reach threshold
 */
internal fun Modifier.forwardEdgeSwipe(
  enabled: Boolean,
  completionThreshold: Float = DEFAULT_COMPLETION_THRESHOLD,
  onProgress: (Float, Offset) -> Unit,
  onCompleted: () -> Unit,
  onCancelled: () -> Unit,
): Modifier {
  if (!enabled) return this
  return this
    .pointerInput(enabled) {
      awaitEachGesture {
        // Wait for first down, only if not consumed by children
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)

        var totalDragX = 0f
        var dragStarted = false

        // Wait for horizontal touch slop - only claim leftward drags
        val drag =
          awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
            // Only claim leftward (negative) drags that aren't consumed
            if (overSlop < 0 && !change.isConsumed) {
              totalDragX = overSlop
              dragStarted = true
              change.consume()
            }
          }

        if (drag != null && dragStarted) {
          val width = size.width.toFloat()
          // Clamp to bounds: can't drag past 0 (right) or -width (full left)
          totalDragX = totalDragX.coerceIn(-width, 0f)

          // Report initial progress
          val initialProgress = if (width > 0f) (abs(totalDragX) / width).coerceIn(0f, 1f) else 0f
          onProgress(initialProgress, Offset(totalDragX, 0f))

          // Track the horizontal drag
          val dragSuccess =
            horizontalDrag(drag.id) { change ->
              val delta = change.positionChange().x
              // Clamp to bounds: can't drag past 0 (right) or -width (full left)
              totalDragX = (totalDragX + delta * FORWARD_DELTA_COEFFICIENT).coerceIn(-width, 0f)
              change.consume()
              // Update progress
              val progress = if (width > 0f) (abs(totalDragX) / width).coerceIn(0f, 1f) else 0f
              onProgress(progress, Offset(totalDragX, 0f))
            }

          // Calculate final progress
          val finalProgress = if (width > 0f) (abs(totalDragX) / width).coerceIn(0f, 1f) else 0f

          if (dragSuccess && finalProgress >= completionThreshold) {
            onCompleted()
          } else {
            onCancelled()
          }

          // Reset progress
          onProgress(0f, Offset.Zero)
        }
      }
    }
}

/** Direction of the gesture-driven navigation. */
enum class GestureDirection {
  /** Backward navigation - sliding content to the right to reveal the previous screen. */
  Backward,

  /** Forward navigation - sliding content to the left as the next screen covers it. */
  Forward,
}

/**
 * A modifier that handles the visual translation and scrim effect during gesture-driven navigation.
 *
 * This modifier handles both backward and forward gestures:
 * - **Backward**: Translates the exiting screen to the right (positive X), applies scrim to the
 *   entering screen that gets lighter as more is revealed
 * - **Forward**: Translates the exiting screen to the left (negative X), applies scrim to the
 *   exiting screen that gets darker as it's covered
 *
 * @param targetState The current navigation state
 * @param transition The enter/exit transition
 * @param direction The direction of the gesture (backward or forward)
 * @param isSeeking Lambda that returns true if the user is currently seeking/swiping
 * @param isGestureActive Lambda that returns true if the gesture navigation is active
 * @param swipeOffset Lambda that returns the current swipe offset
 * @param scrimColor The color to use for the scrim overlay, or null to disable scrim
 */
internal fun Modifier.gestureTranslation(
  targetState: SlideOverTransitionState<*>,
  transition: Transition<EnterExitState>,
  direction: GestureDirection,
  isSeeking: () -> Boolean,
  isGestureActive: () -> Boolean,
  swipeOffset: () -> Offset,
  scrimColor: Color? = Color.Black.copy(alpha = SCRIM_MAX_ALPHA),
): Modifier =
  this then
          GestureTranslationElement(
            targetState = targetState,
            transition = transition,
            direction = direction,
            isSeeking = isSeeking,
            isGestureActive = isGestureActive,
            swipeOffset = swipeOffset,
            scrimColor = scrimColor,
          )

private data class GestureTranslationElement(
  val targetState: SlideOverTransitionState<*>,
  val transition: Transition<EnterExitState>,
  val direction: GestureDirection,
  val isSeeking: () -> Boolean,
  val isGestureActive: () -> Boolean,
  val swipeOffset: () -> Offset,
  val scrimColor: Color?,
) : ModifierNodeElement<GestureTranslationNode>() {

  override fun create(): GestureTranslationNode =
    GestureTranslationNode(
      targetState = targetState,
      transition = transition,
      direction = direction,
      isSeeking = isSeeking,
      isGestureActive = isGestureActive,
      swipeOffset = swipeOffset,
      scrimColor = scrimColor,
    )

  override fun update(node: GestureTranslationNode) {
    node.update(
      targetState = targetState,
      transition = transition,
      direction = direction,
      isSeeking = isSeeking,
      isGestureActive = isGestureActive,
      swipeOffset = swipeOffset,
      scrimColor = scrimColor,
    )
  }
}

/**
 * A node that handles the visual translation and scrim effect during gesture-driven navigation.
 *
 * This node handles two responsibilities based on the transition state and gesture direction:
 *
 * **For backward gestures:**
 * - Exiting screen (PostExit): Applies translation to slide it out to the right
 * - Entering screen (Visible): Applies a scrim overlay that gets lighter as the screen is revealed
 *
 * **For forward gestures:**
 * - Exiting screen (PostExit): Applies translation to slide it out to the left, plus a scrim that
 *   gets darker as it's covered
 * - Entering screen (Visible): Slides in from the right
 *
 * @property targetState The [SlideOverTransitionState] representing the current navigation state.
 * @property transition The [Transition] for the enter/exit state.
 * @property direction The direction of the gesture (backward or forward).
 * @property isSeeking A lambda that returns true if the user is currently seeking/swiping.
 * @property isGestureActive A lambda that returns true if the gesture navigation is active.
 * @property swipeOffset A lambda that returns the current swipe offset.
 * @property scrimColor The color to use for the scrim overlay, or null to disable scrim.
 */
private class GestureTranslationNode(
  private var targetState: SlideOverTransitionState<*>,
  private var transition: Transition<EnterExitState>,
  private var direction: GestureDirection,
  private var isSeeking: () -> Boolean,
  private var isGestureActive: () -> Boolean,
  private var swipeOffset: () -> Offset,
  private var scrimColor: Color?,
) : DelegatingNode(), LayoutModifierNode, DrawModifierNode {

  private val animatable = Animatable(0f)
  private val scrimAnimatable = Animatable(0f)
  private var maxWidth = 0f

  private var layerBlock: GraphicsLayerScope.() -> Unit = {
    // Apply translation only to the exiting screen
    if (transition.targetState == EnterExitState.PostExit) {
      translationX = animatable.value
    }
  }

  override val shouldAutoInvalidate: Boolean = false

  override fun MeasureScope.measure(
    measurable: Measurable,
    constraints: Constraints,
  ): MeasureResult {
    val placeable = measurable.measure(constraints)
    maxWidth = placeable.width.toFloat()
    return layout(placeable.width, placeable.height) {
      placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
    }
  }

  override fun ContentDrawScope.draw() {
    drawContent()

    // Only draw scrim if scrimColor is provided
    val color = scrimColor ?: return

    // Determine if we should show scrim based on direction and transition state
    val shouldShowScrim =
      when (direction) {
        // Backward: scrim on entering screen (Visible) - gets lighter as revealed
        GestureDirection.Backward -> transition.targetState == EnterExitState.Visible
        // Forward: scrim on exiting screen (PostExit) - gets darker as covered
        GestureDirection.Forward -> transition.targetState == EnterExitState.PostExit
      }

    if (shouldShowScrim && scrimAnimatable.value > 0f) {
      val alpha =
        when (direction) {
          // Backward: scrim fades out as screen is revealed (darker when covered)
          GestureDirection.Backward -> 1f - scrimAnimatable.value
          // Forward: scrim fades in as screen is covered (darker as more is covered)
          GestureDirection.Forward -> scrimAnimatable.value
        }
      drawRect(
        color = color.copy(alpha = color.alpha * alpha),
        size = Size(size.width, size.height),
      )
    }
  }

  override fun onAttach() {
    // Handle translation animation for exiting screen
    coroutineScope.launch {
      animatable.snapTo(0f)
      snapshotFlow {
        val offset = swipeOffset()
        val seeking = isSeeking()
        val gestureActive = isGestureActive()

        // Calculate target translation
        val target =
          when {
            !offset.isValid() -> 0f
            seeking -> offset.x
            gestureActive ->
              when (direction) {
                GestureDirection.Backward -> maxWidth
                GestureDirection.Forward -> -maxWidth
              }

            else -> 0f
          }

        // Return pair of (target, shouldSnap)
        // Snap during seeking for immediate response, animate when gesture ends
        target to seeking
      }
        .collectLatest { (target, shouldSnap) ->
          if (shouldSnap) {
            // During seeking, snap to follow finger directly (no animation lag)
            animatable.snapTo(target)
          } else {
            // When gesture ends or resets, animate smoothly
            animatable.animateTo(target)
          }
        }
    }

    // Handle scrim animation
    coroutineScope.launch {
      scrimAnimatable.snapTo(0f)
      snapshotFlow {
        val offset = swipeOffset()
        val seeking = isSeeking()
        val gestureActive = isGestureActive()

        // Calculate target progress
        val target =
          when {
            !offset.isValid() -> 0f
            // Progress is based on how far we've swiped relative to max width
            seeking && maxWidth > 0f -> (abs(offset.x) / maxWidth).coerceIn(0f, 1f)
            gestureActive -> 1f
            else -> 0f
          }

        // Return pair of (target, shouldSnap)
        // Snap during seeking and when resetting to 0
        target to (seeking || target == 0f)
      }
        .collectLatest { (target, shouldSnap) ->
          if (shouldSnap) {
            // During seeking or when resetting, snap immediately
            scrimAnimatable.snapTo(target)
          } else {
            // When gesture completes (gestureActive but not seeking), animate
            scrimAnimatable.animateTo(target)
          }
          invalidateDraw()
        }
    }
  }

  override fun onDetach() {
    maxWidth = 0f
  }

  fun update(
    targetState: SlideOverTransitionState<*>,
    transition: Transition<EnterExitState>,
    direction: GestureDirection,
    isSeeking: () -> Boolean,
    isGestureActive: () -> Boolean,
    swipeOffset: () -> Offset,
    scrimColor: Color?,
  ) {
    this.targetState = targetState
    this.transition = transition
    this.direction = direction
    this.isSeeking = isSeeking
    this.isGestureActive = isGestureActive
    this.swipeOffset = swipeOffset
    this.scrimColor = scrimColor
    this.invalidateLayer()
    this.invalidateDraw()
  }
}

/** Maximum alpha for the scrim overlay (0.32 = 32% opacity black) */
private const val SCRIM_MAX_ALPHA = 0.32f

/** Default threshold for completing the forward gesture (0.5 = 50% of screen width) */
private const val DEFAULT_COMPLETION_THRESHOLD = 0.5f

/**
 * Coefficient applied when calculating the delta for forward navigation animations.
 */
private const val FORWARD_DELTA_COEFFICIENT = 0.8f
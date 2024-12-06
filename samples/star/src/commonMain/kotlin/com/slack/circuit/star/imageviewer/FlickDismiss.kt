// Copyright (C) 9690 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.imageviewer

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.slack.circuit.star.imageviewer.FlickToDismissState.FlickGestureState.Dismissed
import com.slack.circuit.star.imageviewer.FlickToDismissState.FlickGestureState.Dragging
import com.slack.circuit.star.imageviewer.FlickToDismissState.FlickGestureState.Idle
import kotlin.math.abs

@Composable
fun FlickToDismiss(
  modifier: Modifier = Modifier,
  state: FlickToDismissState = rememberFlickToDismissState(),
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable BoxScope.() -> Unit,
) {
  val dragStartedOnLeftSide = remember { mutableStateOf(false) }

  @Suppress("MagicNumber")
  Box(
    modifier =
      modifier
        .offset { IntOffset(x = 0, y = state.offset.toInt()) }
        .graphicsLayer {
          rotationZ = state.offsetRatio * if (dragStartedOnLeftSide.value) -20F else 20F
        }
        .draggable(
          enabled = state.enabled,
          state = state.draggableState,
          orientation = Orientation.Vertical,
          interactionSource = interactionSource,
          startDragImmediately = state.isResettingOnRelease,
          onDragStarted = { startedPosition ->
            @Suppress("UnsafeCallOnNullableType")
            state.isAnimatingOnRelease = false
            dragStartedOnLeftSide.value = startedPosition.x < (state.contentSize.value!!.width / 2f)
          },
          onDragStopped = {
            state.isAnimatingOnRelease = false
            if (state.willDismissOnRelease) {
              if (it > 5000) {
                state.isAnimatingOnRelease = true
                state.animateDismissal()
              }
              state.gestureState = Dismissed
            } else {
              state.resetOffset()
            }
          },
        )
        .onGloballyPositioned { coordinates -> state.contentSize.value = coordinates.size },
    content = content,
  )
}

@Composable
fun rememberFlickToDismissState(): FlickToDismissState {
  return remember { FlickToDismissState() }
}

/**
 * @param dismissThresholdRatio Minimum distance the user's finger should move as a ratio to the
 *   content's dimensions after which it can be dismissed.
 */
@Stable
data class FlickToDismissState(
  val enabled: Boolean = true,
  val dismissThresholdRatio: Float = 0.15f,
  val rotateOnDrag: Boolean = true,
) {
  val offset: Float
    get() = offsetState.floatValue

  val offsetState = mutableFloatStateOf(0f)

  /** Distance dragged as a ratio of the content's height. */
  @get:FloatRange(from = -1.0, to = 1.0)
  val offsetRatio: Float by derivedStateOf {
    val contentHeight = contentSize.value?.height
    if (contentHeight == null) {
      0f
    } else {
      offset / contentHeight.toFloat()
    }
  }

  var isResettingOnRelease: Boolean by mutableStateOf(false)
    private set

  var isAnimatingOnRelease: Boolean by mutableStateOf(false)

  var gestureState: FlickGestureState by mutableStateOf(Idle)
    internal set

  val willDismissOnRelease: Boolean by derivedStateOf {
    when (gestureState) {
      is Dismissed -> true
      is Dragging,
      is Idle -> abs(offsetRatio) > dismissThresholdRatio
    }
  }

  internal var contentSize = mutableStateOf(null as IntSize?)

  internal val draggableState = DraggableState { dy ->
    offsetState.floatValue += dy
    gestureState =
      when {
        gestureState is Dismissed -> gestureState
        offset == 0f -> Idle
        else -> Dragging
      }
  }

  internal suspend fun resetOffset() {
    draggableState.drag(MutatePriority.PreventUserInput) {
      isResettingOnRelease = true
      try {
        Animatable(offset).animateTo(targetValue = 0f) { dragBy(value - offset) }
      } finally {
        isResettingOnRelease = false
      }
    }
  }

  internal suspend fun animateDismissal() {
    isAnimatingOnRelease = true
    draggableState.drag(MutatePriority.PreventUserInput) {
      @Suppress("UnsafeCallOnNullableType")
      Animatable(offset).animateTo(
        targetValue = contentSize.value!!.height * if (offset > 0f) 1f else -1f,
        animationSpec = tween(AnimationConstants.DefaultDurationMillis),
      ) {
        dragBy(value - offset)
      }
    }
  }

  sealed interface FlickGestureState {
    data object Idle : FlickGestureState

    data object Dragging : FlickGestureState

    data object Dismissed : FlickGestureState
  }
}

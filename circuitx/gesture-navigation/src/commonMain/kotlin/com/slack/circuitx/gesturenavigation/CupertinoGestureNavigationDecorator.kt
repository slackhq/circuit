// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Transition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Cupertino specific version of [AnimatedNavDecorator]. This is shipped as common code, to allow
 * easier debugging and testing of this implementation from any platform.
 *
 * @param enterOffsetFraction The fraction (from 0f to 1f) of the entering content's width which the
 *   content starts from. Defaults to 0.25f (25%).
 * @param swipeThreshold The threshold used for determining whether the swipe has 'triggered' a back
 *   event or not. Defaults to 0.4f (40% of the width).
 * @param swipeBackFromNestedScroll Whether nested scroll events should be used to perform gesture
 *   navigation. This is useless when you have full width horizontally scrolling layouts. Defaults
 *   to true.
 */
// todo Assuming a lot about UIKitBackGestureDispatcher, need to verify on iOS.
//  Readd support for nested scroll?
public class CupertinoGestureNavigationDecorator<T : NavArgument>(
  private val enterOffsetFraction: Float = 0.25f,
  private val swipeBackFromNestedScroll: Boolean = true,
  private val onBackInvoked: () -> Unit,
) : PredictiveBackNavigationDecorator<T>(onBackInvoked) {

  // Track popped zIndex so screens are layered correctly
  private var zIndexDepth = 0f

  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return when (animatedNavEvent) {
      AnimatedNavEvent.GoTo -> {
        slideInHorizontally(initialOffsetX = End)
          .togetherWith(
            slideOutHorizontally { width -> -(enterOffsetFraction * width).roundToInt() }
          )
      }
      AnimatedNavEvent.Pop -> {
        slideInHorizontally { width -> -(enterOffsetFraction * width).roundToInt() }
          .togetherWith(
            if (showPrevious) ExitTransition.None else slideOutHorizontally(targetOffsetX = End)
          )
          .apply { targetContentZIndex = --zIndexDepth }
      }
      AnimatedNavEvent.RootReset -> {
        zIndexDepth = 0f
        // Simple Crossfade on reset
        fadeIn() togetherWith fadeOut()
      }
    }
  }

  @Composable
  override fun AnimatedContentScope.Decoration(
    targetState: GestureNavTransitionHolder<T>,
    innerContent: @Composable (T) -> Unit,
  ) {
    Box(
      modifier =
        Modifier.gestureTranslation(
          targetState = targetState,
          transition = transition,
          isSeeking = { isSeeking },
          showPrevious = { showPrevious },
          swipeOffset = { swipeOffset },
        )
    ) {
      innerContent(targetState.args.first())
    }
  }

  public class Factory(
    private val enterOffsetFraction: Float = 0.25f,
    private val swipeBackFromNestedScroll: Boolean = true,
    private val onBackInvoked: () -> Unit,
  ) : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
      return CupertinoGestureNavigationDecorator(
        enterOffsetFraction = enterOffsetFraction,
        swipeBackFromNestedScroll = swipeBackFromNestedScroll,
        onBackInvoked = onBackInvoked,
      )
    }
  }
}

private fun Modifier.gestureTranslation(
  targetState: GestureNavTransitionHolder<*>,
  transition: Transition<EnterExitState>,
  isSeeking: () -> Boolean,
  showPrevious: () -> Boolean,
  swipeOffset: () -> Offset,
): Modifier =
  this then
    GestureTranslationElement(
      targetState = targetState,
      transition = transition,
      isSeeking = isSeeking,
      showPrevious = showPrevious,
      swipeOffset = swipeOffset,
    )

private data class GestureTranslationElement(
  val targetState: GestureNavTransitionHolder<*>,
  val transition: Transition<EnterExitState>,
  val isSeeking: () -> Boolean,
  val showPrevious: () -> Boolean,
  val swipeOffset: () -> Offset,
) : ModifierNodeElement<GestureTranslationNode>() {

  override fun create(): GestureTranslationNode =
    GestureTranslationNode(
      targetState = targetState,
      transition = transition,
      isSeeking = isSeeking,
      showPrevious = showPrevious,
      swipeOffset = swipeOffset,
    )

  override fun update(node: GestureTranslationNode) {
    node.update(
      targetState = targetState,
      transition = transition,
      isSeeking = isSeeking,
      showPrevious = showPrevious,
      swipeOffset = swipeOffset,
    )
  }
}

private class GestureTranslationNode(
  private var targetState: GestureNavTransitionHolder<*>,
  private var transition: Transition<EnterExitState>,
  private var isSeeking: () -> Boolean,
  private var showPrevious: () -> Boolean,
  private var swipeOffset: () -> Offset,
) : DelegatingNode(), LayoutModifierNode {

  private val animatable = Animatable(0f)
  private var maxWidth = 0f

  private var layerBlock: GraphicsLayerScope.() -> Unit = {
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

  override fun onAttach() {
    coroutineScope.launch {
      animatable.snapTo(0f)
      snapshotFlow {
          val offset = swipeOffset()
          when {
            !offset.isValid() -> 0f
            isSeeking() -> offset.x
            showPrevious() -> maxWidth
            else -> 0f
          }
        }
        .collectLatest {
          try {
            animatable.animateTo(it)
          } catch (e: CancellationException) {
            animatable.snapTo(it)
            throw e
          }
        }
    }
  }

  override fun onDetach() {
    maxWidth = 0f
  }

  fun update(
    targetState: GestureNavTransitionHolder<*>,
    transition: Transition<EnterExitState>,
    isSeeking: () -> Boolean,
    showPrevious: () -> Boolean,
    swipeOffset: () -> Offset,
  ) {
    this.targetState = targetState
    this.transition = transition
    this.isSeeking = isSeeking
    this.showPrevious = showPrevious
    this.swipeOffset = swipeOffset
    this.invalidateLayer()
  }
}

private val End: (Int) -> Int = { it }

/** Draggable content for gesture navigation. */
@Composable
private fun DraggableContent(
  state: SwipeDismissState,
  modifier: Modifier = Modifier,
  swipeEnabled: Boolean = true,
  nestedScrollEnabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val nestedScrollConnection = remember(state) { SwipeDismissNestedScrollConnection(state) }
  Box(
    modifier =
      modifier
        .let { if (nestedScrollEnabled) it.nestedScroll(nestedScrollConnection) else it }
        .layout { measurable, constraints ->
          val placeable = measurable.measure(constraints)
          state.maxWidth = constraints.maxWidth.toFloat()
          layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        .draggable(
          state = state.draggableState,
          orientation = Orientation.Horizontal,
          enabled = swipeEnabled,
          reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
          onDragStopped = { velocity -> state.onDragStopped(velocity) },
        )
  ) {
    Box(modifier = Modifier.offset { IntOffset(x = state.offset.roundToInt(), y = 0) }) {
      content()
    }
  }
}

private class SwipeDismissNestedScrollConnection(private val state: SwipeDismissState) :
  NestedScrollConnection {
  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
    when {
      available.x < 0 && source == NestedScrollSource.UserInput -> {
        // If we're being swiped back to origin, let the SwipeDismiss handle it first
        Offset(x = state.performDrag(available.x), y = 0f)
      }
      else -> Offset.Zero
    }

  override fun onPostScroll(
    consumed: Offset,
    available: Offset,
    source: NestedScrollSource,
  ): Offset =
    when (source) {
      NestedScrollSource.UserInput -> Offset(x = state.performDrag(available.x), y = 0f)
      else -> Offset.Zero
    }

  override suspend fun onPreFling(available: Velocity): Velocity =
    when {
      available.x > 0 && state.offset > 0 -> {
        state.onDragStopped(velocity = available.x)
        available
      }
      else -> Velocity.Zero
    }

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
    state.onDragStopped(velocity = available.x)
    return available
  }
}

@Composable
private fun rememberSwipeDismissState(
  key: Any?,
  swipeThreshold: Float,
  onDismissed: () -> Unit,
): SwipeDismissState {
  return remember(key, swipeThreshold) { SwipeDismissState(swipeThreshold) }
    .apply { this.onDismissed = onDismissed }
}

@Stable
private class SwipeDismissState(private val swipeThreshold: Float) {
  var offset by mutableFloatStateOf(0f)
  var maxWidth by mutableFloatStateOf(0f)

  var isDismissed by mutableStateOf(false)
  var onDismissed: () -> Unit = {}

  val progress: Float by derivedStateOf { if (maxWidth == 0f) 0f else offset / maxWidth }

  val draggableState = DraggableState { delta ->
    val newOffset = (offset + delta).coerceIn(0f, maxWidth)
    val resistance = calculateResistance(newOffset)
    offset = newOffset * resistance
  }

  fun performDrag(delta: Float): Float {
    val previousOffset = offset
    val newOffset = (offset + delta).coerceIn(0f, maxWidth)
    val resistance = calculateResistance(newOffset)
    offset = newOffset * resistance
    return offset - previousOffset
  }

  suspend fun onDragStopped(velocity: Float) {
    val thresholdValue = swipeThreshold * maxWidth

    val shouldDismiss = offset >= thresholdValue || velocity > 1000f
    val targetOffset = if (shouldDismiss) maxWidth else 0f

    draggableState.drag(MutatePriority.PreventUserInput) {
      Animatable(offset).animateTo(targetOffset) { dragBy(value - offset) }
    }
    // Only trigger dismiss callback after animation completes
    if (shouldDismiss && targetOffset == maxWidth) {
      isDismissed = true
      onDismissed()
    } else {
      isDismissed = false
    }
    offset = 0f
  }

  private fun calculateResistance(offset: Float): Float {
    return if (offset > maxWidth) {
      val overshoot = offset - maxWidth
      val resistanceFactor = 0.1f
      val resistance = overshoot * resistanceFactor
      (maxWidth + resistance) / offset
    } else {
      1f
    }
  }
}

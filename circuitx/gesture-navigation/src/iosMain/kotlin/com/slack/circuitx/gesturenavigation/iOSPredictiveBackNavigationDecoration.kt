// Copyright (C) 2025 Slack Technologies, LLC
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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.unit.Constraints
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.runtime.Navigator
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Constant for the end offset for slide animations
private val End: (Int) -> Int = { it }

/**
 * A factory that creates an [IOSPredictiveBackNavDecorator] for iOS predictive back navigation.
 *
 * @param fallback The [AnimatedNavDecorator.Factory] to use when predictive back is not supported.
 * @return An [AnimatedNavDecorator.Factory] that provides iOS predictive back navigation.
 */
public actual fun GestureNavigationDecorationFactory(
  fallback: AnimatedNavDecorator.Factory
): AnimatedNavDecorator.Factory {
  return IOSPredictiveBackNavDecorator.Factory()
}

/**
 * iOS implementation of [PredictiveBackNavigationDecorator] that relies on
 * `androidx.compose.ui.backhandler.UIKitBackGestureDispatcher` to perform the predictive back
 * gesture.
 *
 * @property enterOffsetFraction The fraction (from 0f to 1f) of the entering content's width which
 *   the content starts from. Defaults to 0.25f (25%).
 * @param onBackInvoked A callback to be invoked when a back gesture is performed.
 */
internal class IOSPredictiveBackNavDecorator<T : NavArgument>(
  private val enterOffsetFraction: Float = 0.25f,
  onBackInvoked: () -> Unit,
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

  internal class Factory(private val enterOffsetFraction: Float = 0.25f) :
    AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(navigator: Navigator): AnimatedNavDecorator<T, *> {
      return IOSPredictiveBackNavDecorator(
        enterOffsetFraction = enterOffsetFraction,
        onBackInvoked = { navigator.pop() },
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

/**
 * A node that handles the visual translation of content during a back gesture.
 *
 * @property targetState The [GestureNavTransitionHolder] representing the current navigation state.
 * @property transition The [Transition] for the enter/exit state.
 * @property isSeeking A lambda that returns true if the user is currently seeking/swiping.
 * @property showPrevious A lambda that returns true if the previous screen should be shown.
 * @property swipeOffset A lambda that returns the current swipe offset.
 */
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

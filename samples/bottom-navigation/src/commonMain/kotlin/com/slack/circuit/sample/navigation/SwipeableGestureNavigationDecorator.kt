package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuitx.gesturenavigation.GestureNavTransitionHolder
import com.slack.circuitx.gesturenavigation.PredictiveBackNavigationDecorator
import kotlin.math.roundToInt

class SwipeableGestureNavigationDecorator<T : NavArgument>(
  private val adaptiveNavState: AdaptiveNavState,
  private val enterOffsetFraction: Float = 0.25f,
  private val swipeThreshold: Float = 0.4f,
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
            if (adaptiveNavState.isOpen || showPrevious) ExitTransition.None
            else slideOutHorizontally(targetOffsetX = End)
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
    innerContent: @Composable ((T) -> Unit),
  ) {

    val isOpenedScreen =
      targetState.backStackDepth == 2 &&
        targetState.top.screen is SecondaryScreen &&
        adaptiveNavState.isOpen

    val canRestoreOpenScreen = targetState.backStackDepth == 1 && adaptiveNavState.isOpen
    val swipeEnabled = targetState.backStackDepth > 1 || canRestoreOpenScreen

    val dismissState =
      rememberSwipeDismissState(
        targetState.args.first(),
        target = if (isOpenedScreen) Target.Close else Target.Open,
        swipeThreshold = swipeThreshold,
        onRestore = {
          adaptiveNavState.close()
          showPrevious = false
          swipeProgress = 0f
        },
        onDismissed = onBackInvoked,
      )

    LaunchedEffect(isOpenedScreen) {
      if (isOpenedScreen) {
        dismissState.offset = dismissState.maxWidth
        swipeProgress = 1f
        showPrevious = true
      }
    }

    if (swipeEnabled) {
      LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.progress }
          .collect { progress ->
            showPrevious = progress > 0f
            swipeProgress = if (showPrevious) progress else 0f
          }
      }
    }
    DraggableContent(
      state = dismissState,
      swipeEnabled = swipeEnabled,
      content = { innerContent(targetState.args.first()) },
      modifier = Modifier.run {
        if (isOpenedScreen) {
          pointerInput(Unit) {}
        } else this
      }
    )
  }

  class Factory(
    private val adaptiveNavState: AdaptiveNavState,
    private val enterOffsetFraction: Float = 0.25f,
    private val swipeThreshold: Float = 0.4f,
    private val onBackInvoked: () -> Unit,
  ) : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
      return SwipeableGestureNavigationDecorator(
        adaptiveNavState = adaptiveNavState,
        enterOffsetFraction = enterOffsetFraction,
        swipeThreshold = swipeThreshold,
        onBackInvoked = onBackInvoked,
      )
    }
  }
}

private val End: (Int) -> Int = { it }

/** Draggable content for gesture navigation. */
@Composable
private fun DraggableContent(
  state: SwipeDismissState,
  modifier: Modifier = Modifier,
  swipeEnabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  Box(
    modifier =
      modifier
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

@Composable
private fun rememberSwipeDismissState(
  key: Any?,
  target: Target,
  swipeThreshold: Float,
  onRestore: () -> Unit,
  onDismissed: () -> Unit,
): SwipeDismissState {
  return remember(key, target, swipeThreshold) { SwipeDismissState(target, swipeThreshold) }
    .apply {
      this.onRestore = onRestore
      this.onDismissed = onDismissed
    }
}

enum class Target {
  Open,
  Close,
}

@Stable
private class SwipeDismissState(private val target: Target, private val swipeThreshold: Float) {

  var offset by mutableFloatStateOf(0f)
  var maxWidth by mutableFloatStateOf(0f)

  var onRestore: () -> Unit = {}
  var onDismissed: () -> Unit = {}

  val progress: Float by derivedStateOf { if (maxWidth == 0f) 0f else offset / maxWidth }

  val draggableState = DraggableState { delta ->
    val newOffset = (offset + delta).coerceIn(0f, maxWidth)
    val resistance = calculateResistance(newOffset)
    offset = newOffset * resistance
  }

  suspend fun onDragStopped(velocity: Float) {
    when (target) {
      Target.Open -> {
        val thresholdValue = swipeThreshold * maxWidth
        val shouldDismiss = offset >= thresholdValue || velocity > 1000f
        val targetOffset = if (shouldDismiss) maxWidth else 0f

        draggableState.drag(MutatePriority.PreventUserInput) {
          Animatable(offset).animateTo(targetOffset) { dragBy(value - offset) }
        }
        // Only trigger dismiss callback after animation completes
        if (shouldDismiss && targetOffset == maxWidth) {
          onDismissed()
        }
      }
      Target.Close -> {
        val thresholdValue = (1 - swipeThreshold) * maxWidth
        val shouldClose = offset <= thresholdValue || velocity > 1000f
        val targetOffset = if (shouldClose) 0f else maxWidth

        draggableState.drag(MutatePriority.PreventUserInput) {
          Animatable(offset).animateTo(targetOffset) { dragBy(value - offset) }
        }
        // Only trigger restore callback after animation completes
        if (shouldClose && targetOffset == 0f) {
          onRestore()
        }
      }
    }
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

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.EnterExitState.PreEnter
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

// Fraction of the screen that the background screen is offset during transitions.
private const val EnterOffsetFraction = 0.25f
// Duration for child animations. Uses LinearEasing so a gesture's seekTo() fraction maps 1:1 to
// the visual offset. Programmatic navigation animates via animateTo() with no spec, which traverses
// the transition linearly over this same duration.
private const val TransitionDurationMs = 300
// Maximum alpha for the scrim overlay (0.32 = 32% opacity black)
private const val ScrimMaxAlpha = 0.32f
private val TransitionSpec = tween<Float>(TransitionDurationMs, easing = LinearEasing)

/**
 * iOS-style slide-over navigation decorator.
 *
 * The top screen slides in over a dimmed background screen. All visual effects (slide offset,
 * scrim) are driven by a single [SeekableTransitionState]: gestures seek it directly via an
 * [AnchoredDraggableState], while programmatic navigation animates it. Adjacent screens are
 * precomposed ahead of time so swipes stay smooth (see [PreloadTargetStateLayout]).
 *
 * @param canGoForward whether a forward swipe is allowed for the given stack. Defaults to true when
 *   forward history exists.
 * @param canGoBackward whether a backward swipe is allowed for the given stack. Defaults to true
 *   when backward history exists.
 */
public class SlideOverNavDecoration(
  private val canGoForward: (NavStackList<out NavArgument>) -> Boolean = { it.forwardItems.any() },
  private val canGoBackward: (NavStackList<out NavArgument>) -> Boolean = {
    it.backwardItems.any()
  },
) : NavDecoration {
  @OptIn(InternalCircuitApi::class, ExperimentalSharedTransitionApi::class)
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    @Suppress("SlotReused") content: @Composable ((T) -> Unit),
  ) {

    val seekableTransitionState = remember { SeekableTransitionState(args) }
    val transition = rememberTransition(seekableTransitionState, label = "SlideOverNavDecoration")
    val anchoredState = remember(args) { AnchoredDraggableState(SlideOverAnchor.Center) }

    // NavStack states
    val canGoBackward = remember(args) { canGoBackward(args) }
    val canGoForward = remember(args) { canGoForward(args) }
    val previousNavStackList = rememberPrevious(args)
    val nextNavStackList = rememberNext(args)

    val animatedNavEvent =
      remember(transition.currentState, transition.targetState) {
        computeNavEvent(transition.currentState, transition.targetState)
      }

    // Reset for the current active screen
    LaunchedEffect(args) {
      seekableTransitionState.animateTo(args)
    }

    LaunchedEffect(args) {
      snapshotFlow { anchoredState.settledValue }
        .collect { anchor ->
          when (anchor) {
            SlideOverAnchor.Start -> navigator.forward()
            SlideOverAnchor.Center -> Unit
            SlideOverAnchor.End -> navigator.backward()
          }
        }
    }

    LaunchedEffect(previousNavStackList) {
      snapshotFlow {
          anchoredState.progress(SlideOverAnchor.Center, SlideOverAnchor.End)
        }
        .collect { progress ->
          if (progress != 0f && !anchoredState.offset.isNaN()) {
            seekableTransitionState.seekToNav(progress, previousNavStackList)
          }
        }
    }

    LaunchedEffect(nextNavStackList) {
      snapshotFlow {
          anchoredState.progress(SlideOverAnchor.Center, SlideOverAnchor.Start)
        }
        .collect { progress ->
          if (progress != 0f && !anchoredState.offset.isNaN()) {
            seekableTransitionState.seekToNav(progress, nextNavStackList)
          }
        }
    }

    Box(
      modifier =
        modifier
          .anchoredDraggable(
            state = anchoredState,
            orientation = Orientation.Horizontal,
            enabled = canGoBackward || canGoForward,
          )
          .onSizeChanged { intSize ->
            anchoredState.updateAnchors(intSize, canGoForward, canGoBackward)
          }
    ) {
      when (animatedNavEvent) {
        AnimatedNavEvent.GoTo,
        AnimatedNavEvent.Forward -> {
          val backgroundScreen = transition.currentState.active
          val topScreen = transition.targetState.active
          transition.SlideContent(backgroundScreen, topScreen, content)
        }
        AnimatedNavEvent.Pop,
        AnimatedNavEvent.Backward -> {
          val backgroundScreen = transition.targetState.active
          val topScreen = transition.currentState.active
          transition.SlideContent(backgroundScreen, topScreen, content)
        }
        AnimatedNavEvent.RootReset -> {
          transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
          ) { state ->
            ProvideAnimatedTransitionScope(Navigation, this) {
              content(state.active)
            }
          }
        }
        null -> content(transition.currentState.active)
      }
    }
    // Use predictive back handler for system backward gestures
    PredictiveBackEventHandler(
      isEnabled = canGoBackward,
      onBackProgress = { progress, _ ->
        seekableTransitionState.seekToNav(progress, previousNavStackList)
      },
      onBackCancelled = {
        seekableTransitionState.seekTo(1f - seekableTransitionState.fraction, targetState = args)
        seekableTransitionState.animateTo(targetState = args)
      },
      onBackCompleted = {
        navigator.pop()
      },
    )

    var hasSettled by remember { mutableStateOf(true) }
    SideEffect {
      hasSettled =
        anchoredState.targetValue == anchoredState.currentValue &&
          anchoredState.targetValue == anchoredState.settledValue &&
          args == seekableTransitionState.currentState &&
          !transition.isSeeking &&
          !transition.isRunning
    }

    PreloadTargetStateLayout(
      targetState = previousNavStackList?.active,
      shouldPreload = hasSettled && canGoBackward,
      innerContent = content,
    )
    PreloadTargetStateLayout(
      targetState = nextNavStackList?.active,
      shouldPreload = hasSettled && canGoForward,
      innerContent = content,
    )
  }
}

private enum class SlideOverAnchor {
  Start,
  Center,
  End,
}

@Composable
private fun <T : NavArgument> rememberNext(args: NavStackList<T>): NavStackList<T>? =
  remember(args) {
    if (args.forwardItems.any()) {
      val forward = args.forwardItems.drop(1)
      val currentItem = args.forwardItems.first()
      val backward = listOf(args.active) + args.backwardItems
      navStackListOf(forward, currentItem, backward)
    } else null
  }

@Composable
private fun <T : NavArgument> rememberPrevious(args: NavStackList<T>): NavStackList<T>? =
  remember(args) {
    if (args.backwardItems.any()) {
      val forward = listOf(args.active) + args.forwardItems
      val currentItem = args.backwardItems.first()
      val backward = args.backwardItems.drop(1)
      navStackListOf(forward, currentItem, backward)
    } else null
  }

@Composable
private fun <T : NavArgument> Transition<NavStackList<T>>.SlideContent(
  backgroundScreen: T,
  topScreen: T,
  @Suppress("SlotReused") content: @Composable ((T) -> Unit),
) {
  val backgroundSlideOffset =
    animateFloat(
      label = "backgroundSlideOffset",
      transitionSpec = { TransitionSpec },
    ) { state ->
      if (state.active == backgroundScreen) 0f else -EnterOffsetFraction
    }
  val slideOffset =
    animateFloat(
      label = "slideOffset",
      transitionSpec = { TransitionSpec },
    ) { state ->
      if (state.active == topScreen) 0f else 1f
    }
  SlideContentWithTransitionScope(backgroundScreen, backgroundSlideOffset, content)
  Scrim(
    atZeroAlpha = { state ->
      state.active == backgroundScreen
    }
  )
  SlideContentWithTransitionScope(topScreen, slideOffset, content)
}

@Composable
private fun <T> Transition<T>.Scrim(
  atZeroAlpha: (T) -> Boolean,
  modifier: Modifier = Modifier.fillMaxSize(),
) {
  val scrimAlpha =
    animateFloat(
      label = "scrimAlpha",
      transitionSpec = { TransitionSpec },
    ) { state ->
      if (atZeroAlpha(state)) 0f else 1f
    }
  Canvas(modifier = modifier) {
    val alpha = scrimAlpha.value
    if (alpha > 0f) {
      drawRect(Color.Black.copy(alpha = ScrimMaxAlpha * alpha))
    }
  }
}

@Composable
@OptIn(ExperimentalTransitionApi::class, ExperimentalSharedTransitionApi::class)
private fun <T : NavArgument> Transition<NavStackList<T>>.SlideContentWithTransitionScope(
  navArg: T,
  offset: State<Float>,
  content: @Composable ((T) -> Unit),
) {
  Box(modifier = Modifier.graphicsLayer { translationX = offset.value * size.width }) {
    val visibility = createChildTransition { parentState ->
      targetEnterExit(
        visible = {
          it.active == navArg
        },
        targetState = parentState,
      )
    }
    ProvideAnimatedTransitionScope(Navigation, SimpleAnimatedVisibilityScope(visibility)) {
      content(navArg)
    }
  }
}

private fun AnchoredDraggableState<SlideOverAnchor>.updateAnchors(
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

private fun computeNavEvent(
  initialStack: NavStackList<*>,
  targetStack: NavStackList<*>,
): AnimatedNavEvent? {
  val previous = initialStack.active
  val current = targetStack.active

  val initialBackStack = initialStack.backwardItems
  val initialForwardStack = initialStack.forwardItems

  val targetBackStack = targetStack.backwardItems
  val targetForwardStack = targetStack.forwardItems

  return when {
    // Root reset happened.
    initialStack.root != targetStack.root -> {
      AnimatedNavEvent.RootReset
    }
    // Target screen has not changed, don't show an animation.
    initialStack == targetStack -> {
      null
    }
    // Navigated backward with the screen moving to the forward stack.
    current in initialBackStack &&
      previous !in initialForwardStack &&
      previous in targetForwardStack -> {
      AnimatedNavEvent.Backward
    }
    // Popped the screen off the nav stack.
    current in initialBackStack && previous !in targetForwardStack -> {
      AnimatedNavEvent.Pop
    }
    // Navigated forward with the screen moving out of the forward stack.
    current in initialForwardStack && current !in targetForwardStack -> {
      AnimatedNavEvent.Forward
    }
    // Fallback to a normal GoTo.
    else -> {
      AnimatedNavEvent.GoTo
    }
  }
}

private suspend fun <T : NavArgument> SeekableTransitionState<NavStackList<T>>.seekToNav(
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

/** A [AnimatedVisibilityScope] that takes a [Transition]. */
private data class SimpleAnimatedVisibilityScope(
  override val transition: Transition<EnterExitState>
) : AnimatedVisibilityScope

// This converts Boolean visible to EnterExitState
@Composable
private fun <T> Transition<T>.targetEnterExit(
  visible: (T) -> Boolean,
  targetState: T,
): EnterExitState =
  key(this) {
    if (this.isSeeking) {
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

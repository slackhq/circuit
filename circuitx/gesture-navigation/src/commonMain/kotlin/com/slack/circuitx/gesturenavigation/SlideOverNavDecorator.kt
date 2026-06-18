// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeLayoutState.PausedPrecomposition
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Fraction of the screen that the background screen is offset during transitions.
private const val EnterOffsetFraction = 0.25f
// Duration for child animations. Uses LinearEasing so seekTo() maps 1:1.
// The parent animateTo() spec provides easing for programmatic navigation.
private const val TransitionDurationMs = 300
// Maximum alpha for the scrim overlay (0.32 = 32% opacity black)
private const val ScrimMaxAlpha = 0.32f
// Quick animation for cancelling of the slide over
private val CancelSpec = tween<Float>(durationMillis = 80, easing = LinearEasing)

/**
 * Factory that creates a [SlideOverNavDecorator] for iOS-style slide-over navigation.
 *
 * This decorator provides:
 * - Full-screen slide animations (screens slide completely in/out)
 * - Gesture-driven backward navigation (swipe from left edge to go back)
 * - Gesture-driven forward navigation (swipe from right edge to go forward in history)
 *
 * @param scrimColor The color to use for the scrim overlay during gestures
 */
class SlideOverNavDecoratorFactory(private val scrimColor: Color = Color.Black) :
  AnimatedNavDecorator.Factory {
  override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> =
    SlideOverNavDecorator(scrimColor = scrimColor)
}

/**
 * Wraps the current [navStack] with precomputed [previous] and [next] states for gesture seeking.
 *
 * [previous] and [next] represent the nav stack as it would look after a backward or forward
 * navigation respectively, allowing [SeekableTransitionState] to seek toward either without
 * modifying the real stack.
 */
@Immutable
class SlideOverTransitionState<T : NavArgument>(
  override val navStack: NavStackList<T>,
  val previous: SlideOverTransitionState<T>? = null,
  val next: SlideOverTransitionState<T>? = null,
) : AnimatedNavState {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as SlideOverTransitionState<*>
    return navStack == other.navStack
  }

  override fun hashCode(): Int {
    return navStack.hashCode()
  }
}

@Stable
private class SwipeState(val direction: GestureDirection) {
  var progress: Float by mutableFloatStateOf(0f)
}

/**
 * iOS-style slide-over navigation decorator with a unified animation system.
 *
 * All visual effects (slide offset, scrim) are driven by [SeekableTransitionState]:
 * - **Gestures**: `seekTo(fraction)` with `LinearEasing` child specs = 1:1 finger tracking
 * - **Programmatic nav**: `animateTo(target)` = natural-feeling animation
 *
 * @param scrimColor The color to use for the scrim overlay during gestures
 */
private class SlideOverNavDecorator<T : NavArgument>(private val scrimColor: Color) :
  AnimatedNavDecorator<T, SlideOverTransitionState<T>> {

  private lateinit var seekableTransitionState: SeekableTransitionState<SlideOverTransitionState<T>>
  private val swipeState = mutableStateMapOf<SlideOverTransitionState<T>, SwipeState>()
  private var zIndexDepth = 0f
  private var lastNavEvent: AnimatedNavEvent? by mutableStateOf(null)
  private lateinit var navigator: Navigator

  override fun updateNavigator(navigator: Navigator) {
    this.navigator = navigator
  }

  override fun targetState(args: NavStackList<T>): SlideOverTransitionState<T> {
    // Build the state we'd go to if we go backwards (previous screen)
    val previous =
      if (args.backwardItems.any()) {
        val forward = listOf(args.active) + args.forwardItems
        val currentItem = args.backwardItems.first()
        val backward = args.backwardItems.drop(1)
        SlideOverTransitionState(navStackListOf(forward, currentItem, backward))
      } else null

    // Build the state we'd go to if we go forwards (next screen in history)
    val next =
      if (args.forwardItems.any()) {
        val forward = args.forwardItems.drop(1)
        val currentItem = args.forwardItems.first()
        val backward = listOf(args.active) + args.backwardItems
        SlideOverTransitionState(navStackListOf(forward, currentItem, backward))
      } else null

    return SlideOverTransitionState(args, previous, next)
  }

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun updateTransition(args: NavStackList<T>): Transition<SlideOverTransitionState<T>> {
    val current = remember(args) { targetState(args) }

    seekableTransitionState = remember { SeekableTransitionState(current) }

    LaunchedEffect(current) {
      seekableTransitionState.animateTo(current)
      swipeState.clear()
    }

    // Handle gesture seeking
    LaunchedEffect(current) {
      snapshotFlow {
          val state = swipeState[current]
          if (state != null) {
            when (state.direction) {
              GestureDirection.Backward -> current.previous
              GestureDirection.Forward -> current.next
            }?.let { it to state.progress }
          } else null
        }
        .filterNotNull()
        .collect { (target, progress) ->
          try {
            seekableTransitionState.seekTo(fraction = abs(progress), targetState = target)
          } catch (_: CancellationException) {
            // If seekTo is interrupted we want to keep observing the swipeState
          }
        }
    }

    // Use predictive back handler for backward gestures (left edge swipe)
    PredictiveBackEventHandler(
      isEnabled = current.previous != null,
      onBackProgress = { progress, _ ->
        swipeState
          .getOrPut(current) { SwipeState(direction = GestureDirection.Backward) }
          .progress = abs(progress)
      },
      onBackCancelled = {
        swipeState.remove(current)
        seekableTransitionState.animateTo(targetState = current, animationSpec = CancelSpec)
      },
      onBackCompleted = {
        navigator.pop()
        swipeState.remove(current)
      },
    )

    return rememberTransition(seekableTransitionState, label = "SlideOverNavDecorator")
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    lastNavEvent = animatedNavEvent
    return when (animatedNavEvent) {
      AnimatedNavEvent.Forward,
      AnimatedNavEvent.GoTo -> {
        EnterTransition.None togetherWith ExitTransition.None
      }
      AnimatedNavEvent.Backward,
      AnimatedNavEvent.Pop -> {
        (EnterTransition.None togetherWith ExitTransition.None).apply {
          targetContentZIndex = --zIndexDepth
        }
      }
      AnimatedNavEvent.RootReset -> {
        zIndexDepth = 0f
        fadeIn() togetherWith fadeOut()
      }
    }
  }

  @Suppress("SlotReused") // Slot is in mutually exclusive if/else branches
  @Composable
  override fun AnimatedContentScope.Decoration(
    targetState: SlideOverTransitionState<T>,
    innerContent: @Composable (T) -> Unit,
  ) {
    val scope = rememberCoroutineScope()
    val canGoForward = targetState.navStack.forwardItems.any()
    val canGoBackward = targetState.navStack.backwardItems.any()
    PreloadForwardComposition(
      targetState = targetState,
      canGoForward = canGoForward,
      hasSettled = transition.targetState == transition.currentState,
      innerContent = innerContent,
    )

    val slideOffset by
      transition.animateFloat(
        label = "slideOffset",
        transitionSpec = { tween(TransitionDurationMs, easing = LinearEasing) },
      ) { state ->
        when (lastNavEvent) {
          AnimatedNavEvent.Forward,
          AnimatedNavEvent.GoTo ->
            when (state) {
              EnterExitState.PreEnter -> 1f // entering from right edge
              EnterExitState.Visible -> 0f // at center
              EnterExitState.PostExit -> -EnterOffsetFraction // exiting to partial left
            }
          AnimatedNavEvent.Backward,
          AnimatedNavEvent.Pop ->
            when (state) {
              EnterExitState.PreEnter -> -EnterOffsetFraction // entering from partial left
              EnterExitState.Visible -> 0f // at center
              EnterExitState.PostExit -> 1f // exiting to right edge
            }
          AnimatedNavEvent.RootReset,
          null -> 0f
        }
      }

    // Scrim alpha for the background screen (0 = no scrim, 1 = full scrim).
    val scrimAlpha by
      transition.animateFloat(
        label = "scrimAlpha",
        transitionSpec = { tween(TransitionDurationMs, easing = LinearEasing) },
      ) { state ->
        when (lastNavEvent) {
          AnimatedNavEvent.Forward,
          AnimatedNavEvent.GoTo ->
            when (state) {
              EnterExitState.PreEnter -> 0f // entering foreground: no scrim
              EnterExitState.Visible -> 0f
              EnterExitState.PostExit -> 1f // exiting background: full scrim
            }
          AnimatedNavEvent.Backward,
          AnimatedNavEvent.Pop ->
            when (state) {
              EnterExitState.PreEnter -> 1f // entering background: full scrim initially
              EnterExitState.Visible -> 0f // revealed: no scrim
              EnterExitState.PostExit -> 0f // exiting foreground: no scrim
            }
          AnimatedNavEvent.RootReset,
          null -> 0f
        }
      }

    Box(
      modifier =
        Modifier.slideOverGesture(
            canGoBackward = canGoBackward,
            canGoForward = canGoForward,
            onProgress = { direction, progress ->
              swipeState.getOrPut(targetState) { SwipeState(direction) }.progress = progress
            },
            onCompleted = { direction ->
              when (direction) {
                GestureDirection.Backward -> navigator.pop()
                GestureDirection.Forward -> navigator.forward()
              }
              swipeState.remove(targetState)
            },
            onCancelled = {
              scope.launch {
                swipeState.remove(targetState)
                val current = targetState(targetState.navStack)
                seekableTransitionState.animateTo(targetState = current, animationSpec = CancelSpec)
              }
            },
          )
          .graphicsLayer { translationX = slideOffset * size.width }
          .drawWithContent {
            drawContent()
            if (scrimAlpha > 0f) {
              drawRect(scrimColor.copy(alpha = ScrimMaxAlpha * scrimAlpha))
            }
          }
    ) {
      // Pause the exiting presenter while animating to reduce some overhead.
      val isExiting = transition.targetState == EnterExitState.PostExit
      ProvideRecordLifecycle(isActive = !isExiting) { innerContent(targetState.navStack.active) }
    }
  }
}

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
@Suppress("SlotReused") // Slot is in mutually exclusive if/else branches
@Composable
private fun <T : NavArgument> PreloadForwardComposition(
  targetState: SlideOverTransitionState<T>,
  canGoForward: Boolean,
  hasSettled: Boolean,
  innerContent: @Composable ((T) -> Unit),
) {
  val composer = currentComposer
  val targetStateToPreload = targetState.next
  var lastPausedComposition by remember { mutableStateOf<PausedPrecomposition?>(null) }
  var lastPreloadState by remember { mutableStateOf<SlideOverTransitionState<T>?>(null) }

  // Immediately cancel on any change!
  if (targetStateToPreload != lastPreloadState) {
    lastPausedComposition?.cancel()
    lastPausedComposition = null
  }

  if (hasSettled && canGoForward && targetStateToPreload != null) {
    lastPreloadState = targetStateToPreload
    val subcomposeLayoutState = remember(targetStateToPreload) { SubcomposeLayoutState() }

    LaunchedEffect(subcomposeLayoutState) {
      var isActive = true
      val pausedComposition =
        subcomposeLayoutState
          .createPausedPrecomposition(targetStateToPreload.navStack.active.key) {
            // Keep active while this loads, and then lazily pause the presenter.
            ProvideRecordLifecycle(isActive = isActive) {
              innerContent(targetStateToPreload.navStack.active)
            }
          }
          .also { lastPausedComposition = it }

      // Loop until preload is complete.
      while (!pausedComposition.isComplete) {
        pausedComposition.resume {
          composer.composition.let { it.hasPendingChanges || it.isComposing }
        }
        // Wait for the frame before continuing
        withFrameNanos {}
      }
      val handle = pausedComposition.apply()
      isActive = false
      // Wait and cleanup the handle when the effect is disposed.
      suspendCancellableCoroutine { handle.dispose() }
    }
    SubcomposeLayout(subcomposeLayoutState) { constraints ->
      val completed = lastPausedComposition?.isComplete == true
      if (completed) {
        val measurable =
          subcompose(targetStateToPreload.navStack.active.key) {
              ProvideRecordLifecycle(isActive = false) {
                innerContent(targetStateToPreload.navStack.active)
              }
            }
            .first()
        val placeable = measurable.measure(constraints)
        val size = placeable.let { constraints.constrain(IntSize(it.width, it.height)) }
        layout(size.width, size.height) { placeable.place(size.width, 0) }
      } else {
        layout(0, 0) {}
      }
    }
  }
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import com.slack.circuit.foundation.internal.PredictiveBackEventHandler
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Factory that creates a [SlideOverNavDecorator] for iOS-style slide-over navigation.
 *
 * This decorator provides:
 * - Full-screen slide animations (screens slide completely in/out)
 * - Gesture-driven backward navigation (swipe from left edge to go back)
 * - Gesture-driven forward navigation (swipe from right edge to go forward in history)
 *
 * @param onBackInvoked Callback invoked when a backward gesture completes
 * @param onForwardInvoked Callback invoked when a forward gesture completes
 * @param scrimColor The color to use for the scrim overlay during gestures, or null to disable
 */
class SlideOverNavDecoratorFactory(
  private val onBackInvoked: () -> Unit,
  private val onForwardInvoked: () -> Unit = {},
  private val scrimColor: Color? = Color.Black.copy(alpha = 0.32f),
) : AnimatedNavDecorator.Factory {
  override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> =
    SlideOverNavDecorator(
      onBackInvoked = onBackInvoked,
      onForwardInvoked = onForwardInvoked,
      scrimColor = scrimColor,
    )
}

/**
 * A holder class used by the `AnimatedContent` composables. This enables us to pass through all of
 * the necessary information as an argument, which is optimal for `AnimatedContent`.
 *
 * @property navStack The current navigation stack
 * @property previous The state we'd transition to if going backward (null if no backward history)
 * @property next The state we'd transition to if going forward (null if no forward history)
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

/**
 * iOS-style slide-over navigation decorator that provides full-screen slide animations and
 * gesture-driven navigation in both directions.
 *
 * @param onBackInvoked Callback invoked when a backward gesture completes
 * @param onForwardInvoked Callback invoked when a forward gesture completes
 * @param scrimColor The color to use for the scrim overlay during gestures, or null to disable
 */
class SlideOverNavDecorator<T : NavArgument>(
  private val onBackInvoked: () -> Unit,
  private val onForwardInvoked: () -> Unit = {},
  private val scrimColor: Color? = Color.Black.copy(alpha = 0.32f),
) : AnimatedNavDecorator<T, SlideOverTransitionState<T>> {

  private lateinit var seekableTransitionState: SeekableTransitionState<SlideOverTransitionState<T>>

  private var showPrevious: Boolean by mutableStateOf(false)
  private var showNext: Boolean by mutableStateOf(false)
  private var isSeeking: Boolean by mutableStateOf(false)
  private var swipeProgress: Float by mutableFloatStateOf(0f)
  private var swipeOffset: Offset by mutableStateOf(Offset.Zero)

  // Direction of current gesture
  private var direction: GestureDirection by mutableStateOf(GestureDirection.Backward)

  // Track popped zIndex so screens are layered correctly
  private var zIndexDepth = 0f

  // Fraction of the screen that the entering screen is offset during transitions
  // This creates the overlapping effect where screens slide over each other
  private val enterOffsetFraction = 0.25f

  override fun targetState(args: NavStackList<T>): SlideOverTransitionState<T> {
    // Build the state we'd go to if we go backwards (previous screen)
    val previous =
      if (args.backwardItems.iterator().hasNext()) {
        val forward = listOf(args.active) + args.forwardItems
        val currentItem = args.backwardItems.first()
        val backward = args.backwardItems.drop(1)
        SlideOverTransitionState(navStackListOf(forward, currentItem, backward))
      } else null

    // Build the state we'd go to if we go forwards (next screen in history)
    val next =
      if (args.forwardItems.iterator().hasNext()) {
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
      swipeProgress = 0f
      isSeeking = false
      seekableTransitionState.animateTo(current)
      // After the current state has changed (i.e. any transition has completed),
      // clear out any transient state
      showPrevious = false
      showNext = false
      swipeOffset = Offset.Zero
    }

    // Handle backward gesture seeking
    LaunchedEffect(current.previous, current) {
      current.previous?.let { previous ->
        snapshotFlow {
            when (direction) {
              GestureDirection.Backward -> swipeProgress
              GestureDirection.Forward -> 0f
            }
          }
          .collect { progress ->
            if (progress != 0f) {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = previous)
            }
          }
      }
    }

    // Handle forward gesture seeking
    LaunchedEffect(current.next, current) {
      current.next?.let { next ->
        snapshotFlow {
            when (direction) {
              GestureDirection.Backward -> 0f
              GestureDirection.Forward -> swipeProgress
            }
          }
          .collect { progress ->
            if (progress != 0f) {
              isSeeking = true
              seekableTransitionState.seekTo(fraction = abs(progress), targetState = next)
            }
          }
      }
    }

    // Use predictive back handler for backward gestures (left edge swipe)
    PredictiveBackEventHandler(
      isEnabled = current.previous != null,
      onBackProgress = { progress, offset ->
        direction = GestureDirection.Backward
        showNext = false
        showPrevious = progress != 0f
        swipeProgress = abs(progress)
        swipeOffset = offset
      },
      onBackCancelled = {
        isSeeking = false
        seekableTransitionState.animateTo(current)
      },
      onBackCompleted = { onBackInvoked() },
    )

    return rememberTransition(seekableTransitionState, label = "SlideOverNavDecorator")
  }

  override fun AnimatedContentTransitionScope<AnimatedNavState>.transitionSpec(
    animatedNavEvent: AnimatedNavEvent
  ): ContentTransform {
    return when (animatedNavEvent) {
      // Forward (GoTo, Forward event): New screen slides in from right (full width),
      // current slides partially to the left (creates overlapping effect).
      // During gesture (showNext), use ExitTransition.None since gesture handles translation.
      AnimatedNavEvent.Forward,
      AnimatedNavEvent.GoTo -> {
        val enterTransition =
          if (isSeeking) {
            EnterTransition.None
          } else {
            slideInHorizontally { it }
          }
        val exitTransition = slideOutHorizontally { width ->
          -(enterOffsetFraction * width).roundToInt()
        }
        enterTransition.togetherWith(exitTransition)
      }
      // Backward (Pop, Backward event): Previous screen slides in from partial left offset,
      // current screen slides out to the right (full width).
      // During gesture (showPrevious), use ExitTransition.None since gesture handles translation.
      AnimatedNavEvent.Backward,
      AnimatedNavEvent.Pop -> {
        val enterTransition = slideInHorizontally { width ->
          -(enterOffsetFraction * width).roundToInt()
        }
        val exitTransition =
          if (showPrevious) {
            ExitTransition.None
          } else {
            slideOutHorizontally { it }
          }
        enterTransition.togetherWith(exitTransition).apply { targetContentZIndex = --zIndexDepth }
      }
      // RootReset: Crossfade
      AnimatedNavEvent.RootReset -> {
        zIndexDepth = 0f
        fadeIn() togetherWith fadeOut()
      }
    }
  }

  @Composable
  override fun AnimatedContentScope.Decoration(
    targetState: SlideOverTransitionState<T>,
    innerContent: @Composable (T) -> Unit,
  ) {
    val scope = rememberCoroutineScope()
    val canGoForward = targetState.navStack.forwardItems.any()
    Box(
      modifier =
        Modifier.forwardEdgeSwipe(
            enabled = canGoForward,
            onProgress = { progress, offset ->
              direction = GestureDirection.Forward
              showPrevious = false
              showNext = progress != 0f
              swipeProgress = progress
              swipeOffset = offset
            },
            onCompleted = { onForwardInvoked() },
            onCancelled = {
              isSeeking = false
              scope.launch { seekableTransitionState.animateTo(targetState) }
            },
          )
          .gestureTranslation(
            transition = transition,
            direction = direction,
            isSeeking = { isSeeking },
            isGestureActive = {
              when (direction) {
                GestureDirection.Backward -> showPrevious
                GestureDirection.Forward -> showNext
              }
            },
            swipeOffset = { swipeOffset },
            scrimColor = scrimColor,
          )
    ) {
      // Record is active if it is in the animation
      ProvideRecordLifecycle(true) { innerContent(targetState.navStack.active) }
    }
  }
}

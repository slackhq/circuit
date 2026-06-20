// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.slack.circuit.foundation.NavDecoration
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
import kotlin.collections.set
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

// Fraction of the screen that the background screen is offset during transitions.
private const val EnterOffsetFraction = 0.25f
// Duration for child animations. Uses LinearEasing so seekTo() maps 1:1.
// The parent animateTo() spec provides easing for programmatic navigation.
private const val TransitionDurationMs = 300
// Maximum alpha for the scrim overlay (0.32 = 32% opacity black)
private const val ScrimMaxAlpha = 0.32f
// Quick animation for cancelling of the slide over
private val CancelSpec = tween<Float>(durationMillis = 8000, easing = LinearEasing)
private val TransitionSpec = tween<Float>(TransitionDurationMs, easing = LinearEasing)

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
public class SlideOverNavDecoratorFactory(private val scrimColor: Color = Color.Black) :
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
private class SlideOverTransitionState<T : NavArgument>(
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

  val progress = Channel<Float>()

  fun cancel() {
    progress.cancel()
  }
}

/**
 * iOS-style slide-over navigation decorator with a unified animation system.
 *
 * All visual effects (slide offset, scrim) are driven by [SeekableTransitionState]:
 * - **Gestures**: `seekTo(fraction)` with `LinearEasing` child specs = 1:1 finger tracking
 * - **Programmatic nav**: `animateTo(target)` = natural-feeling animation
 */
public class SlideOverNavDecoration(
  private val canGoForward: (NavStackList<out NavArgument>) -> Boolean = { it.forwardItems.any() },
  private val canGoBackward: (NavStackList<out NavArgument>) -> Boolean = {
    it.backwardItems.any()
  },
) : NavDecoration {
  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    @Suppress("SlotReused") content: @Composable ((T) -> Unit),
  ) {
    val scope = rememberCoroutineScope()
    val seekableTransitionState = remember { SeekableTransitionState(args) }
    val transition = rememberTransition(seekableTransitionState, label = "SlideOverNavDecoration")
    var isSeeking by remember { mutableStateOf(false) }

    val previousNavStackList =
      remember(args) {
        if (args.backwardItems.any()) {
          val forward = listOf(args.active) + args.forwardItems
          val currentItem = args.backwardItems.first()
          val backward = args.backwardItems.drop(1)
          navStackListOf(forward, currentItem, backward)
        } else null
      }
    val nextNavStackList =
      remember(args) {
        if (args.forwardItems.any()) {
          val forward = args.forwardItems.drop(1)
          val currentItem = args.forwardItems.first()
          val backward = listOf(args.active) + args.backwardItems
          navStackListOf(forward, currentItem, backward)
        } else null
      }
    val canGoBackward = canGoBackward(args)
    val canGoForward = canGoForward(args)

    LaunchedEffect(args) {
      isSeeking = false
      seekableTransitionState.animateTo(args)
    }

    val animatedNavEvent =
      remember(transition.currentState, transition.targetState) {
        computeNavEvent(transition.currentState, transition.targetState)
      }

    var job by remember { mutableStateOf<Job?>(null) }

    Box(
      modifier =
        modifier.slideOverGesture(
          canGoBackward = canGoBackward,
          canGoForward = canGoForward,
          onProgress = { direction, progress ->
            job?.cancel()
            job = scope.launch {
              when (direction) {
                GestureDirection.Backward -> {
                  if (previousNavStackList != null) {
                    try {
                      isSeeking = true
                      seekableTransitionState.seekTo(
                        fraction = abs(progress),
                        targetState = previousNavStackList,
                      )
                    } catch (_: CancellationException) {
                      // If seekTo is interrupted we want to keep observing the swipeState
                    }
                  }
                }
                GestureDirection.Forward -> {
                  if (nextNavStackList != null) {
                    try {
                      isSeeking = true
                      seekableTransitionState.seekTo(
                        fraction = abs(progress),
                        targetState = nextNavStackList,
                      )
                    } catch (_: CancellationException) {
                      // If seekTo is interrupted we want to keep observing the swipeState
                    }
                  }
                }
              }
            }
          },
          onCompleted = { direction ->
            isSeeking = false
            when (direction) {
              GestureDirection.Backward -> navigator.backward()
              GestureDirection.Forward -> navigator.forward()
            }
          },
          onCancelled = { _ ->
            scope.launch {
              seekableTransitionState.seekTo(
                1f - seekableTransitionState.fraction,
                targetState = args,
              )
              seekableTransitionState.animateTo(targetState = args)
              isSeeking = false
            }
          },
        )
    ) {
      when (animatedNavEvent) {
        AnimatedNavEvent.GoTo,
        AnimatedNavEvent.Forward -> {
          val backgroundScreen = transition.currentState.active
          val topScreen = transition.targetState.active

          val backgroundSlideOffset by
            transition.animateFloat(
              label = "backgroundSlideOffset",
              transitionSpec = { TransitionSpec },
            ) { state ->
              if (state.active == backgroundScreen) 0f else -EnterOffsetFraction
            }
          // Scrim alpha for the background screen (0 = no scrim, 1 = full scrim).
          val slideOffset by
            transition.animateFloat(
              label = "slideOffset",
              transitionSpec = { tween(TransitionDurationMs, easing = LinearEasing) },
            ) { state ->
              if (state.active == topScreen) 0f else 1f
            }
          Box(
            modifier = Modifier.graphicsLayer { translationX = backgroundSlideOffset * size.width }
          ) {
            content(backgroundScreen)
          }
          transition.Scrim(
            atZeroAlpha = { state ->
              state.active == backgroundScreen
            }
          )
          Box(modifier = Modifier.graphicsLayer { translationX = slideOffset * size.width }) {
            content(topScreen)
          }
        }
        AnimatedNavEvent.Pop,
        AnimatedNavEvent.Backward -> {
          val backgroundScreen = transition.targetState.active
          val topScreen = transition.currentState.active

          val backgroundSlideOffset by
            transition.animateFloat(
              label = "backgroundSlideOffset",
              transitionSpec = { TransitionSpec },
            ) { state ->
              if (state.active == backgroundScreen) 0f else -EnterOffsetFraction
            }
          val slideOffset by
            transition.animateFloat(
              label = "slideOffset",
              transitionSpec = { tween(TransitionDurationMs, easing = LinearEasing) },
            ) { state ->
              if (state.active == topScreen) 0f else 1f
            }
          Box(
            modifier = Modifier.graphicsLayer { translationX = backgroundSlideOffset * size.width }
          ) {
            content(backgroundScreen)
          }
          transition.Scrim(
            atZeroAlpha = { state ->
              state.active == backgroundScreen
            }
          )
          Box(modifier = Modifier.graphicsLayer { translationX = slideOffset * size.width }) {
            content(topScreen)
          }
        }
        AnimatedNavEvent.RootReset -> {
          transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
          ) { state ->
            content(state.active)
          }
        }
        null -> content(transition.currentState.active)
      }
    }
    // Use predictive back handler for backward gestures (left edge swipe)
    PredictiveBackEventHandler(
      isEnabled = canGoBackward,
      onBackProgress = { progress, _ ->
        if (previousNavStackList != null) {
          try {
            isSeeking = true
            seekableTransitionState.seekTo(
              fraction = abs(progress),
              targetState = previousNavStackList,
            )
          } catch (_: CancellationException) {
            // If seekTo is interrupted we want to keep observing the swipeState
          }
        }
      },
      onBackCancelled = {
        seekableTransitionState.seekTo(1f - seekableTransitionState.fraction, targetState = args)
        seekableTransitionState.animateTo(targetState = args)
        isSeeking = false
      },
      onBackCompleted = {
        navigator.pop()
        isSeeking = false
      },
    )

    val hasSettled =
      isSeeking &&
        args == seekableTransitionState.currentState &&
        !transition.isSeeking &&
        !transition.isRunning

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

private class SlideOverNavDecorator<T : NavArgument>(private val scrimColor: Color) :
  AnimatedNavDecorator<T, SlideOverTransitionState<T>> {

  private lateinit var seekableTransitionState: SeekableTransitionState<SlideOverTransitionState<T>>
  private val swipeStates = mutableStateMapOf<SlideOverTransitionState<T>, SwipeState>()
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

  @OptIn(InternalCircuitApi::class, ExperimentalCoroutinesApi::class)
  @Composable
  override fun updateTransition(args: NavStackList<T>): Transition<SlideOverTransitionState<T>> {
    val current = remember(args) { targetState(args) }

    seekableTransitionState = remember { SeekableTransitionState(current) }

    LaunchedEffect(current) {
      seekableTransitionState.animateTo(current)
      swipeStates.clear()
    }

    // Handle gesture seeking
    LaunchedEffect(current) {
      snapshotFlow {
          swipeStates[current]
        }
        .transformLatest { swipeState ->
          val targetState =
            when (swipeState?.direction) {
              GestureDirection.Backward -> current.previous
              GestureDirection.Forward -> current.next
              null -> null
            }
          if (targetState != null && swipeState != null) {
            for (progress in swipeState.progress) {
              emit(targetState to progress)
            }
          }
        }
        .collect { (target, progress) ->
          try {
            seekableTransitionState.seekTo(fraction = abs(progress), targetState = target)
          } catch (_: CancellationException) {
            // If seekTo is interrupted we want to keep observing the swipeState
          }
        }
    }

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

  @Suppress("SlotReused") // innerContent slot is in mutually exclusive if/else branches
  @Composable
  override fun AnimatedContentScope.Decoration(
    targetState: SlideOverTransitionState<T>,
    innerContent: @Composable (T) -> Unit,
  ) {
    val scope = rememberCoroutineScope()
    val canGoForward = targetState.navStack.forwardItems.any()
    val canGoBackward = targetState.navStack.backwardItems.any()
    val isInBackground = isInBackground(transition.targetState, lastNavEvent)

    PreloadTargetStateLayout(
      targetState = targetState.navStack.active,
      shouldPreload = canGoForward && transition.targetState == transition.currentState,
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
        if (isInBackground(state, lastNavEvent)) 1f else 0f
      }

    Box(
      modifier =
        Modifier.slideOverGesture(
            canGoBackward = canGoBackward,
            canGoForward = canGoForward,
            onProgress = { direction, progress ->
              val swipeSate = swipeStates[targetState]
              if (swipeSate?.direction == direction) {
                swipeSate.progress.trySend(progress)
              } else {
                swipeSate?.cancel()
                swipeStates[targetState] =
                  SwipeState(direction).also {
                    it.progress.trySend(progress)
                  }
              }
            },
            onCompleted = { direction ->
              val swipeSate = swipeStates[targetState]
              if (swipeSate?.direction == direction) {
                when (direction) {
                  GestureDirection.Backward -> navigator.pop()
                  GestureDirection.Forward -> navigator.forward()
                }
                swipeStates.remove(targetState)
                swipeSate.cancel()
              }
            },
            onCancelled = { direction ->
              val swipeSate = swipeStates[targetState]
              if (swipeSate?.direction == direction) {
                swipeStates.remove(targetState)
                swipeSate.cancel()
                scope.launch {
                  val current = targetState(targetState.navStack)
                  seekableTransitionState.animateTo(
                    targetState = current,
                    animationSpec = CancelSpec,
                  )
                }
              }
            },
          )
          // Block touch when in the background
          .pointerInput(isInBackground, swipeStates[targetState]) {
            if (isInBackground && swipeStates[targetState] == null) awaitEachGesture {}
          }
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
    previous == current -> {
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

@Stable
private fun isInBackground(enterExitState: EnterExitState, navEvent: AnimatedNavEvent?): Boolean =
  when (navEvent) {
    AnimatedNavEvent.Forward,
    AnimatedNavEvent.GoTo ->
      when (enterExitState) {
        EnterExitState.PreEnter -> false // entering foreground
        EnterExitState.Visible -> false
        EnterExitState.PostExit -> true // exiting background
      }

    AnimatedNavEvent.Backward,
    AnimatedNavEvent.Pop ->
      when (enterExitState) {
        EnterExitState.PreEnter -> true // entering background
        EnterExitState.Visible -> false // revealed: no scrim
        EnterExitState.PostExit -> false // exiting foreground
      }

    AnimatedNavEvent.RootReset,
    null -> false
  }

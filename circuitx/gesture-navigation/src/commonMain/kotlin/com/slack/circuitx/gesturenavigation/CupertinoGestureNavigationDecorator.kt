// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("DEPRECATION") // TODO migrate!

package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.ThresholdConfig
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import com.slack.circuit.backstack.NavArgument
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedNavEvent
import com.slack.circuit.foundation.animation.AnimatedNavState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter

/**
 * Cupertino specific version of [AnimatedNavDecorator]. This is shipped as common code, to allow
 * easier debugging and testing of this implementation from any platform.
 *
 * @param enterOffsetFraction The fraction (from 0f to 1f) of the entering content's width which the
 *   content starts from. Defaults to 0.25f (25%).
 * @param swipeThreshold The threshold used for determining whether the swipe has 'triggered' a back
 *   event or not. Defaults a swipe of at least 40% of the width.
 * @param swipeBackFromNestedScroll Whether nested scroll events should be used to perform gesture
 *   navigation. This is useless when you have full width horizontally scrolling layouts. Defaults
 *   to true.
 */
@ExperimentalMaterialApi
public class CupertinoGestureNavigationDecorator<T : NavArgument>(
  private val enterOffsetFraction: Float = 0.25f,
  private val swipeThreshold: ThresholdConfig = FractionalThreshold(0.4f),
  private val swipeBackFromNestedScroll: Boolean = true,
  private val onBackInvoked: () -> Unit,
) : AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  private lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>

  private var showPrevious by mutableStateOf(false)
  private var swipeProgress by mutableFloatStateOf(0f)

  // Track popped zIndex so screens are layered correctly
  private var zIndexDepth = 0f

  override fun targetState(args: ImmutableList<T>): GestureNavTransitionHolder<T> {
    return GestureNavTransitionHolder(args)
  }

  @Composable
  override fun updateTransition(args: ImmutableList<T>): Transition<GestureNavTransitionHolder<T>> {

    val current = remember(args) { targetState(args) }
    val previous =
      remember(args) {
        if (args.size > 1) {
          targetState(args.subList(1, args.size))
        } else null
      }

    seekableTransitionState = remember { SeekableTransitionState(current) }

    LaunchedEffect(current) {
      // When the current state has changed (i.e. any transition has completed),
      // clear out any transient state
      showPrevious = false
      swipeProgress = 0f
      seekableTransitionState.animateTo(current)
    }

    LaunchedEffect(previous, current) {
      if (previous != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
              seekableTransitionState.seekTo(fraction = progress, targetState = previous)
            }
          }
      }
    }

    LaunchedEffect(showPrevious) {
      if (!showPrevious) {
        // If the previous was shown but not dismissed make sure seekableTransitionState is reset
        // correctly.
        seekableTransitionState.animateTo(current)
      }
    }

    return rememberTransition(
      seekableTransitionState,
      label = "CupertinoGestureNavigationDecorator",
    )
  }

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
    val dismissState = rememberDismissState(targetState.args.first())
    var wasSwipeDismissed by remember { mutableStateOf(false) }
    val swipeEnabled = targetState.backStackDepth > 1

    LaunchedEffect(dismissState) {
      snapshotFlow { dismissState.isDismissed(DismissDirection.StartToEnd) }
        .filter { it }
        .collect {
          onBackInvoked()
          wasSwipeDismissed = dismissState.offset.value != 0f
        }
    }

    if (swipeEnabled) {
      LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.progress }
          .collect { progress ->
            showPrevious =
              progress.to == DismissValue.DismissedToEnd && progress.from == DismissValue.Default
            swipeProgress = if (showPrevious) abs(progress.fraction) else 0f
          }
      }
    }

    if (!wasSwipeDismissed) {
      SwipeableContent(
        state = dismissState,
        swipeEnabled = swipeEnabled,
        nestedScrollEnabled = swipeEnabled && swipeBackFromNestedScroll,
        dismissThreshold = swipeThreshold,
        content = { innerContent(targetState.args.first()) },
      )
    }
  }

  public class Factory(
    private val enterOffsetFraction: Float = 0.25f,
    private val swipeThreshold: ThresholdConfig = FractionalThreshold(0.4f),
    private val swipeBackFromNestedScroll: Boolean = true,
    private val onBackInvoked: () -> Unit,
  ) : AnimatedNavDecorator.Factory {
    override fun <T : NavArgument> create(): AnimatedNavDecorator<T, *> {
      return CupertinoGestureNavigationDecorator(
        enterOffsetFraction = enterOffsetFraction,
        swipeThreshold = swipeThreshold,
        swipeBackFromNestedScroll = swipeBackFromNestedScroll,
        onBackInvoked = onBackInvoked,
      )
    }
  }
}

private val End: (Int) -> Int = { it }

/** This is basically [androidx.compose.material.SwipeToDismiss] but simplified for our use case. */
@Composable
@ExperimentalMaterialApi
private fun SwipeableContent(
  state: DismissState,
  dismissThreshold: ThresholdConfig,
  modifier: Modifier = Modifier,
  swipeEnabled: Boolean = true,
  nestedScrollEnabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  BoxWithConstraints(modifier) {
    val width = constraints.maxWidth

    val nestedScrollConnection = remember(state) { SwipeDismissNestedScrollConnection(state) }

    Box(
      modifier =
        Modifier.let { if (nestedScrollEnabled) it.nestedScroll(nestedScrollConnection) else it }
          .swipeable(
            state = state,
            anchors =
              mapOf(0f to DismissValue.Default, width.toFloat() to DismissValue.DismissedToEnd),
            thresholds = { _, _ -> dismissThreshold },
            orientation = Orientation.Horizontal,
            enabled = swipeEnabled,
            reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
            resistance =
              ResistanceConfig(
                basis = width.toFloat(),
                factorAtMin = SwipeableDefaults.StiffResistanceFactor,
                factorAtMax = SwipeableDefaults.StandardResistanceFactor,
              ),
          )
    ) {
      Box(modifier = Modifier.offset { IntOffset(x = state.offset.value.roundToInt(), y = 0) }) {
        content()
      }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
private class SwipeDismissNestedScrollConnection(private val state: DismissState) :
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
      available.x > 0 && state.offset.value > 0 -> {
        state.performFling(velocity = available.x)
        available
      }
      else -> Velocity.Zero
    }

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
    state.performFling(velocity = available.x)
    return available
  }
}

@Composable
@ExperimentalMaterialApi
private fun rememberDismissState(
  vararg inputs: Any?,
  initialValue: DismissValue = DismissValue.Default,
  confirmStateChange: (DismissValue) -> Boolean = { true },
): DismissState {
  return rememberSaveable(*inputs, saver = DismissState.Saver(confirmStateChange)) {
    DismissState(initialValue, confirmStateChange)
  }
}

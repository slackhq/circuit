// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import com.slack.circuit.backstack.NavDecoration
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter

/**
 * Configuration properties for [GestureNavigationDecoration].
 *
 * @property enterOffsetFraction The fraction (from 0f to 1f) of the entering content's width which
 *   the content starts from. Defaults to 0.25f (25%).
 * @property swipeThreshold The threshold used for determining whether the swipe has 'triggered' a
 *   back event or not. Defaults a swipe of at least 40% of the width.
 * @property swipeBackFromNestedScroll Whether nested scroll events should be used to perform
 *   gesture navigation. This is useless when you have full width horizontally scrolling layouts.
 *   Defaults to true.
 */
@ExperimentalMaterialApi
@Immutable
public class GestureNavigationProperties(
  public val enterOffsetFraction: Float = 0.25f,
  public val swipeThreshold: ThresholdConfig = FractionalThreshold(0.4f),
  public val swipeBackFromNestedScroll: Boolean = true,
)

@OptIn(ExperimentalMaterialApi::class)
public actual fun GestureNavigationDecoration(
  onBack: () -> Unit,
): NavDecoration = gestureNavigationDecoration(onBack, GestureNavigationProperties())

@ExperimentalMaterialApi
public fun gestureNavigationDecoration(
  onBack: () -> Unit,
  properties: GestureNavigationProperties,
): NavDecoration = IosGestureNavigationDecoration(onBack, properties)

@ExperimentalMaterialApi
private class IosGestureNavigationDecoration(
  val onBack: () -> Unit,
  val properties: GestureNavigationProperties,
) : NavDecoration {

  @Composable
  override fun <T> DecoratedContent(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    val current = args.first()
    val previous = args.getOrNull(1)

    Box(modifier = modifier) {
      // Remember the previous stack depth so we know if the navigation is going "back".
      var prevStackDepth by rememberSaveable { mutableStateOf(backStackDepth) }
      SideEffect { prevStackDepth = backStackDepth }

      val dismissState = rememberDismissState(current)
      var offsetWhenPopped by remember { mutableStateOf(0f) }

      LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.isDismissed(DismissDirection.StartToEnd) }
          .filter { it }
          .collect {
            onBack()
            offsetWhenPopped = dismissState.offset.value
          }
      }

      val transition = updateTransition(targetState = current, label = "GestureNavDecoration")

      if (previous != null) {
        // Previous content is only visible if the swipe-dismiss offset != 0
        val showPrevious by
          remember(dismissState) {
            derivedStateOf { dismissState.offset.value != 0f || transition.isRunning }
          }

        PreviousContent(
          isVisible = { showPrevious },
          modifier =
            Modifier.graphicsLayer {
              translationX =
                when {
                  // If we're running in a transition, let it handle any translation
                  transition.isRunning -> 0f
                  else -> {
                    // Otherwise we'll react to the swipe dismiss state
                    (dismissState.offset.value.absoluteValue - size.width) *
                      properties.enterOffsetFraction
                  }
                }
            },
          content = { content(previous) },
        )
      }

      transition.AnimatedContent(
        transitionSpec = {
          when {
            // adding to back stack
            backStackDepth > prevStackDepth -> {
              slideInHorizontally(
                  initialOffsetX = End,
                )
                .togetherWith(
                  slideOutHorizontally { width ->
                    -(properties.enterOffsetFraction * width).roundToInt()
                  },
                )
            }

            // come back from back stack
            backStackDepth < prevStackDepth -> {
              if (offsetWhenPopped != 0f) {
                // If the record change was caused by a swipe gesture, let's
                // jump cut
                EnterTransition.None togetherWith ExitTransition.None
              } else {
                slideInHorizontally { width ->
                    -(properties.enterOffsetFraction * width).roundToInt()
                  }
                  .togetherWith(
                    slideOutHorizontally(targetOffsetX = End),
                  )
                  .apply { targetContentZIndex = -1f }
              }
            }

            // Root reset. Crossfade
            else -> fadeIn() togetherWith fadeOut()
          }
        },
        modifier = modifier,
      ) { record ->
        SwipeableContent(
          state = dismissState,
          swipeEnabled = backStackDepth > 1,
          nestedScrollEnabled = backStackDepth > 1 && properties.swipeBackFromNestedScroll,
          dismissThreshold = properties.swipeThreshold,
          content = { content(record) },
        )
      }

      LaunchedEffect(current) {
        // Reset the offsetWhenPopped when the top record changes
        offsetWhenPopped = 0f
      }
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
              mapOf(
                0f to DismissValue.Default,
                width.toFloat() to DismissValue.DismissedToEnd,
              ),
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
          ),
    ) {
      Box(
        modifier = Modifier.offset { IntOffset(x = state.offset.value.roundToInt(), y = 0) },
      ) {
        content()
      }
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
private class SwipeDismissNestedScrollConnection(
  private val state: DismissState,
) : NestedScrollConnection {
  override fun onPreScroll(
    available: Offset,
    source: NestedScrollSource,
  ): Offset =
    when {
      available.x < 0 && source == NestedScrollSource.Drag -> {
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
      NestedScrollSource.Drag -> Offset(x = state.performDrag(available.x), y = 0f)
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

  override suspend fun onPostFling(
    consumed: Velocity,
    available: Velocity,
  ): Velocity {
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
  return rememberSaveable(inputs, saver = DismissState.Saver(confirmStateChange)) {
    DismissState(initialValue, confirmStateChange)
  }
}

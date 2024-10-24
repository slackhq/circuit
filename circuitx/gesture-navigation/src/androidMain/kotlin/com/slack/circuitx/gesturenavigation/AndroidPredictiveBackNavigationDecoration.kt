// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.gesturenavigation

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.slack.circuit.backstack.NavDecoration
import com.slack.circuit.foundation.AnimatedNavDecorator
import com.slack.circuit.foundation.DefaultAnimatedNavDecoration
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.SharedElementTransitionScope
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

public actual fun GestureNavigationDecoration(
  fallback: NavDecoration,
  onBackInvoked: () -> Unit,
): NavDecoration =
  when {
    Build.VERSION.SDK_INT >= 34 -> AndroidPredictiveBackNavigationDecoration(onBackInvoked)
    else -> fallback
  }

@RequiresApi(34)
public class AndroidPredictiveBackNavigationDecoration(private val onBackInvoked: () -> Unit) :
  NavDecoration by DefaultAnimatedNavDecoration(
    AndroidPredictiveBackNavDecorator.Factory(onBackInvoked)
  )

@Suppress("SlotReused") // This is an advanced use case
@RequiresApi(34)
internal class AndroidPredictiveBackNavDecorator<T>(private val onBackInvoked: () -> Unit) :
  AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  private lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>
  private var isSharedTransitionActive by mutableStateOf(false)
  private var showPrevious by mutableStateOf(false)
  private var swipeProgress by mutableFloatStateOf(0f)

  private var backStackDepthState by mutableIntStateOf(0)
  private var currentHolder by mutableStateOf<GestureNavTransitionHolder<T>?>(null)
  private var previousHolder by mutableStateOf<GestureNavTransitionHolder<T>?>(null)

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  override fun Content(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (Transition<GestureNavTransitionHolder<T>>.(Modifier) -> Unit),
  ) {
    val scope = rememberStableCoroutineScope()
    val current =
      remember(args) {
        args
          .first()
          .let { GestureNavTransitionHolder(it, backStackDepth, args.last()) }
          .also { currentHolder = it }
      }
    val previous =
      remember(args) {
        args
          .getOrNull(1)
          ?.let { GestureNavTransitionHolder(it, backStackDepth - 1, args.last()) }
          ?.also { previousHolder = it }
      }

    backStackDepthState = backStackDepth
    seekableTransitionState = remember { SeekableTransitionState(current) }
    val transition = rememberTransition(seekableTransitionState, label = "GestureNavDecoration")

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

    if (backStackDepth > 1) {
      BackHandler(
        onBackProgress = { progress ->
          showPrevious = progress != 0f
          swipeProgress = progress
        },
        onBackCancelled = { scope.launch { seekableTransitionState.snapTo(current) } },
        onBackInvoked = { onBackInvoked() },
      )
    }

    if (SharedElementTransitionScope.isAvailable()) {
      // todo We don't know this fast enough for the transitionSpec
      SharedElementTransitionScope { isSharedTransitionActive = isTransitionActive }
    }

    transition.content(modifier)
  }

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun Transition<GestureNavTransitionHolder<T>>.transitionSpec():
    AnimatedContentTransitionScope<GestureNavTransitionHolder<T>>.() -> ContentTransform = {
    val diff = targetState.backStackDepth - initialState.backStackDepth
    val sameRoot = targetState.rootRecord == initialState.rootRecord

    when {
      isSharedTransitionActive -> EnterTransition.None togetherWith ExitTransition.None
      // adding to back stack
      sameRoot && diff > 0 -> NavigatorDefaults.DefaultDecoration.forward
      // come back from back stack
      sameRoot && diff < 0 -> {
        if (showPrevious) {
            EnterTransition.None togetherWith scaleOut(targetScale = 0.8f) + fadeOut()
          } else {
            NavigatorDefaults.DefaultDecoration.backward
          }
          .apply { targetContentZIndex = -1f }
      }
      // Root reset. Crossfade
      else -> fadeIn() togetherWith fadeOut()
    }
  }

  @Composable
  override fun AnimatedContentScope.AnimatedNavContent(
    targetState: GestureNavTransitionHolder<T>,
    content: @Composable (T) -> Unit,
  ) {
    Box(
      Modifier.predictiveBackMotion(
        shape = MaterialTheme.shapes.extraLarge,
        progress = {
          if (
            !isSharedTransitionActive &&
              swipeProgress != 0f &&
              seekableTransitionState.currentState == targetState
          ) {
            swipeProgress
          } else 0f
        },
      )
    ) {
      content(targetState.record)
    }
  }

  class Factory(private val onBackInvoked: () -> Unit) : AnimatedNavDecorator.Factory {
    override fun <T> create(): AnimatedNavDecorator<T, *> {
      return AndroidPredictiveBackNavDecorator(onBackInvoked = onBackInvoked)
    }
  }
}

/**
 * Implements most of the treatment specified at
 * https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#designing-gesture
 *
 * The only piece missing is the vertical shift.
 */
private fun Modifier.predictiveBackMotion(shape: Shape, progress: () -> Float): Modifier =
  graphicsLayer {
    val p = progress()
    // If we're at progress 0f, skip setting any parameters
    if (p == 0f) return@graphicsLayer

    translationX = -(8.dp * p).toPx()
    shadowElevation = 6.dp.toPx()

    val scale = lerp(1f, 0.9f, p.absoluteValue)
    scaleX = scale
    scaleY = scale
    transformOrigin = TransformOrigin(pivotFractionX = if (p > 0) 1f else 0f, pivotFractionY = 0.5f)

    this.shape = shape
    clip = true
  }

@RequiresApi(34)
@Composable
private fun BackHandler(
  onBackProgress: (Float) -> Unit,
  onBackCancelled: () -> Unit,
  animatedEnabled: Boolean = true,
  onBackInvoked: () -> Unit,
) {
  val onBackDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("OnBackPressedDispatcher is not available")
  val lastAnimatedEnabled by rememberUpdatedState(animatedEnabled)
  val lastOnBackProgress by rememberUpdatedState(onBackProgress)
  val lastOnBackCancelled by rememberUpdatedState(onBackCancelled)
  val lastOnBackInvoked by rememberUpdatedState(onBackInvoked)

  DisposableEffect(onBackDispatcher) {
    val callback =
      object : OnBackPressedCallback(true) {

        override fun handleOnBackStarted(backEvent: BackEventCompat) {
          if (lastAnimatedEnabled) {
            lastOnBackProgress(0f)
          }
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
          if (lastAnimatedEnabled) {
            lastOnBackProgress(
              when (backEvent.swipeEdge) {
                BackEvent.EDGE_LEFT -> backEvent.progress
                else -> -backEvent.progress
              }
            )
          }
        }

        override fun handleOnBackCancelled() {
          if (lastAnimatedEnabled) {
            lastOnBackCancelled()
          }
        }

        override fun handleOnBackPressed() = lastOnBackInvoked()
      }

    onBackDispatcher.addCallback(callback)

    onDispose { callback.remove() }
  }
}

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
import androidx.compose.runtime.derivedStateOf
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
import com.slack.circuit.foundation.LocalSharedElementTransitionState
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.SharedElementTransitionScope
import com.slack.circuit.foundation.SharedElementTransitionState
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
    Build.VERSION.SDK_INT >= 34 ->
      DefaultAnimatedNavDecoration(AndroidPredictiveBackNavDecorator.Factory(onBackInvoked))
    else -> fallback
  }

@RequiresApi(34)
internal class AndroidPredictiveBackNavDecorator<T>(private val onBackInvoked: () -> Unit) :
  AnimatedNavDecorator<T, GestureNavTransitionHolder<T>> {

  private lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>

  private var recordPoppedFromGesture by mutableStateOf<T?>(null)

  private var swipeProgress by mutableFloatStateOf(0f)
  private var currentHolder by mutableStateOf<GestureNavTransitionHolder<T>?>(null)
  private var previousHolder by mutableStateOf<GestureNavTransitionHolder<T>?>(null)
  private var backStackDepth by mutableIntStateOf(0)
  private var isTransitionActive by mutableStateOf(false)

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  override fun Content(
    args: ImmutableList<T>,
    backStackDepth: Int,
    modifier: Modifier,
    content: @Composable (Transition<GestureNavTransitionHolder<T>>.(Modifier) -> Unit),
  ) {
    this.backStackDepth = backStackDepth
    currentHolder =
      remember(args) { GestureNavTransitionHolder(args.first(), backStackDepth, args.last()) }
    previousHolder =
      remember(args) {
        args.getOrNull(1)?.let { GestureNavTransitionHolder(it, backStackDepth - 1, args.last()) }
      }
    seekableTransitionState = remember { SeekableTransitionState(currentHolder!!) }

    val transition =
      rememberTransition(transitionState = seekableTransitionState, label = "GestureNavDecoration")

    val scope = rememberStableCoroutineScope()
    LaunchedEffect(currentHolder) {
      swipeProgress = 0f
      seekableTransitionState.animateTo(currentHolder!!)
    }
    LaunchedEffect(previousHolder, currentHolder) {
      val previousHolder = previousHolder
      if (previousHolder != null) {
        snapshotFlow { swipeProgress }
          .collect { progress ->
            if (progress != 0f) {
              seekableTransitionState.seekTo(fraction = progress, targetState = previousHolder)
            }
          }
      }
    }

    if (backStackDepth > 1) {
      BackHandler(
        onBackProgress = { swipeProgress = it },
        onBackCancelled = { scope.launch { seekableTransitionState.snapTo(currentHolder!!) } },
        onBackInvoked = { onBackInvoked() },
      )
    }

    Box(modifier = modifier) {
      if (LocalSharedElementTransitionState.current == SharedElementTransitionState.Available) {
        SharedElementTransitionScope {
          this@AndroidPredictiveBackNavDecorator.isTransitionActive = isTransitionActive
          transition.content(Modifier)
        }
      } else {
        transition.content(Modifier)
      }
    }
  }

  @OptIn(InternalCircuitApi::class)
  @Composable
  override fun Transition<GestureNavTransitionHolder<T>>.transitionSpec():
    AnimatedContentTransitionScope<GestureNavTransitionHolder<T>>.() -> ContentTransform = {
    val diff = targetState.backStackDepth - initialState.backStackDepth
    val sameRoot = targetState.rootRecord == initialState.rootRecord

    when {
      isTransitionActive -> fadeIn() togetherWith fadeOut()
      // adding to back stack
      sameRoot && diff > 0 -> NavigatorDefaults.DefaultDecoration.forward
      // come back from back stack
      sameRoot && diff < 0 -> {
        if (recordPoppedFromGesture == initialState.record) {
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
          if (swipeProgress != 0f && seekableTransitionState.currentState == targetState) {
            swipeProgress
          } else 0f
        },
      )
    ) {
      // todo Do we want to provide seekable progress to the content?
      val contentProgress = remember {
        derivedStateOf {
          if (seekableTransitionState.targetState == targetState) {
            seekableTransitionState.fraction
          } else {
            1f - seekableTransitionState.fraction
          }
        }
      }
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

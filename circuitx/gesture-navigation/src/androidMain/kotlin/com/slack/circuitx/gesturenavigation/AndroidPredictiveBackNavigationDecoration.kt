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
import com.slack.circuit.foundation.FractionSharedAnimatedVisibilityScope
import com.slack.circuit.foundation.LocalSharedElementTransitionState
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.ProvideAnimatedTransitionScope
import com.slack.circuit.foundation.SharedElementTransitionScope
import com.slack.circuit.foundation.SharedElementTransitionScope.AnimatedScope.Navigation
import com.slack.circuit.foundation.SharedElementTransitionState
import com.slack.circuit.runtime.InternalCircuitApi
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList

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

  private var showPrevious by mutableStateOf(false)

  private var recordPoppedFromGesture by mutableStateOf<T?>(null)

  //  private var seekableTransitionState by
  //    mutableStateOf<SeekableTransitionState<GestureNavTransitionHolder<T>>?>(null)
  //  private var currentHolder by mutableStateOf<GestureNavTransitionHolder<T>?>(null)
  //  private var previousHolder by mutableStateOf<GestureNavTransitionHolder<T>?>(null)

  private lateinit var currentHolder: GestureNavTransitionHolder<T>
  private var previousHolder: GestureNavTransitionHolder<T>? = null
  private lateinit var seekableTransitionState:
    SeekableTransitionState<GestureNavTransitionHolder<T>>
  private var backStackDepth = 0
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
    seekableTransitionState = remember { SeekableTransitionState(currentHolder) }

    Box(modifier = modifier) {
      LaunchedEffect(currentHolder) { seekableTransitionState.animateTo(currentHolder) }

      val transition =
        rememberTransition(
          transitionState = seekableTransitionState,
          label = "GestureNavDecoration",
        )

      LaunchedEffect(transition.currentState) {
        // When the current state has changed (i.e. any transition has completed),
        // clear out any transient state
        showPrevious = false
        recordPoppedFromGesture = null
      }

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

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  override fun AnimatedContentScope.AnimatedNavContent(
    targetState: GestureNavTransitionHolder<T>,
    content: @Composable (T) -> Unit,
  ) {
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    if (backStackDepth > 1 && targetState.backStackDepth == currentHolder.backStackDepth) {
      BackHandler(
        onBackProgress = { progress ->
          showPrevious = progress != 0f
          swipeProgress = progress
        },
        onBackInvoked = {
          if (swipeProgress != 0f) {
            // If back has been invoked, and the swipe progress isn't zero,
            // mark this record as 'popped via gesture' so we can
            // use a different transition
            recordPoppedFromGesture = targetState.record
          }
          onBackInvoked()
        },
      )
      val previousHolder = previousHolder
      if (showPrevious && previousHolder != null && previousHolder != targetState) {
        LaunchedEffect(previousHolder) {
          snapshotFlow { swipeProgress }
            .collect { progress ->
              seekableTransitionState.seekTo(fraction = progress, targetState = previousHolder)
            }
        }
      }
    }

    Box(
      Modifier.predictiveBackMotion(
        shape = MaterialTheme.shapes.extraLarge,
        progress = {
          if (showPrevious && previousHolder != targetState) {
            swipeProgress
          } else 0f
        },
      )
    ) {
      val contentProgress = remember {
        derivedStateOf {
          if (seekableTransitionState.targetState == targetState) {
            seekableTransitionState.fraction
          } else {
            1f - seekableTransitionState.fraction
          }
        }
      }

      val visibilityScope = remember {
        FractionSharedAnimatedVisibilityScope(this@AnimatedNavContent, contentProgress)
      }
      ProvideAnimatedTransitionScope(Navigation, visibilityScope) { content(targetState.record) }
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
  animatedEnabled: Boolean = true,
  onBackInvoked: () -> Unit,
) {
  val onBackDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("OnBackPressedDispatcher is not available")
  val lastAnimatedEnabled by rememberUpdatedState(animatedEnabled)
  val lastOnBackProgress by rememberUpdatedState(onBackProgress)
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

        override fun handleOnBackPressed() = lastOnBackInvoked()
      }

    onBackDispatcher.addCallback(callback)

    onDispose { callback.remove() }
  }
}

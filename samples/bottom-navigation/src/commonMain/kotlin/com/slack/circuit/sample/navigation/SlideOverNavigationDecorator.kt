// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onLayoutRectChanged
import com.slack.circuit.foundation.NavArgument
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.animation.AnimatedNavDecorator
import com.slack.circuit.foundation.animation.AnimatedScreenTransform
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.InternalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Navigator.StateOptions
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

@OptIn(ExperimentalCircuitApi::class)
class SlideOverNavDecoration(
  screenTransforms: Map<KClass<out Screen>, AnimatedScreenTransform>,
  private val decoratorFactory: AnimatedNavDecorator.Factory,
  private val slideOverNavState: SlideOverNavState,
  private val showInDetailPane: (NavArgument) -> Boolean,
  private val scrimColor: @Composable () -> Color,
  private val backgroundColor: @Composable () -> Color,
) : NavDecoration {

  private val delegate =
    AnimatedNavDecoration(
      animatedScreenTransforms = screenTransforms,
      decoratorFactory = decoratorFactory,
    )

  @OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalSharedTransitionApi::class,
    InternalCircuitApi::class,
  )
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: List<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable ((T) -> Unit),
  ) {
    DisposableEffect(Unit) {
      slideOverNavState.isDecorating = true
      onDispose { slideOverNavState.isDecorating = false }
    }
    val (primaryArgs, secondaryLookup) = rememberListDetailNavArguments(args, showInDetailPane)

    val primary = primaryArgs.first()
    val secondary = secondaryLookup[primary]?.singleOrNull()
    val detailLookupTransition = updateTransition(primary to secondary)
    val delegateArgs = if (secondary != null) primaryArgs else args

    delegate.DecoratedContent(delegateArgs, navigator, modifier) { base ->
      val detail =
        with(detailLookupTransition) {
          val (currentBase, currentDetail) = currentState
          val (targetBase, targetDetail) = targetState
          when {
            // todo Fix composition timings
            currentBase == base -> currentDetail
            targetBase == base -> targetDetail
            else -> null
          }
        }

      // TODO State and somehow use the decorator animations
      var maxOffset by remember { mutableFloatStateOf(0f) }
      val offset = remember(detail?.key) { Animatable(0f) }
      val scope = rememberCoroutineScope()
      // todo Real drag handling with velocity etc
      val dragState = rememberDraggableState { drag ->
        scope.launch { offset.snapTo((offset.value + drag).coerceIn(0f, maxOffset)) }
      }
      var isDragging by remember { mutableStateOf(false) }
      val isOffScreen = slideOverNavState.isOffScreen(detail?.screen)
      var wasOffScreen by remember { mutableStateOf(isOffScreen) }
      LaunchedEffect(isOffScreen) {
        when {
          isOffScreen -> offset.snapTo(maxOffset)
          wasOffScreen -> offset.animateTo(0f)
          else -> offset.snapTo(0f)
        }
        wasOffScreen = isOffScreen
      }

      Box(
        modifier =
          Modifier.onLayoutRectChanged { maxOffset = it.width.toFloat() }
            .draggable(
              state = dragState,
              enabled = isOffScreen,
              orientation = Orientation.Horizontal,
              onDragStarted = { isDragging = true },
              onDragStopped = {
                if (it > maxOffset / 0.4f) { //  swipeThreshold: Float = 0.4f,
                  offset.animateTo(maxOffset)
                } else {
                  offset.animateTo(0f)
                  slideOverNavState.setOnScreen(detail?.screen)
                }
                isDragging = false
              },
            )
      ) {

        // Base
        val scrimColor = scrimColor()
        Box(
          modifier =
            Modifier.graphicsLayer {
                if (detail != null) {
                  val percentage = offset.value / maxOffset
                  val max =
                    (maxOffset * .25f).coerceAtLeast(0f) // enterOffsetFraction: Float = 0.25f,
                  translationX = -max + (percentage * max).coerceIn(0f, max)
                } else {
                  translationX = 0f
                }
              }
              .drawWithContent {
                drawContent()
                if (detail != null) {
                  val max = maxOffset * .6f //  swipeThreshold: Float = 0.4f,
                  val percentage = (offset.value / max).coerceIn(0f, 1f)
                  val alpha = scrimColor.alpha - (scrimColor.alpha * percentage)
                  drawRect(
                    color = scrimColor,
                    size = size.copy(width = size.width * 1.25f),
                    alpha = alpha,
                  )
                }
              }
              .run {
                if (detail != null && maxOffset == 0f) {
                  pointerInput(Unit) {}
                } else this
              }
        ) {
          content(base)
        }

        // SlideOver
        AnimatedVisibility(
          visible = detail != null,
          modifier = Modifier.fillMaxSize(),
          enter =
            if (isOffScreen) EnterTransition.None else NavigatorDefaults.forward.targetContentEnter,
          exit =
            if (isOffScreen) ExitTransition.None else NavigatorDefaults.backward.initialContentExit,
        ) {
          if (detail != null) {
            ProvideAnimatedTransitionScope(Navigation, this) {
              val backgroundColor = backgroundColor()
              Box(
                modifier =
                  Modifier.graphicsLayer { translationX = offset.value }
                    .background(backgroundColor)
                    .run {
                      if (maxOffset != 0f) {
                        pointerInput(Unit) {}
                      } else this
                    }
              ) {
                content(detail)
              }
              PredictiveBackHandler(enabled = !isOffScreen) { progress ->
                try {
                  progress.collect { backEvent ->
                    val progress = backEvent.progress * maxOffset
                    offset.snapTo(progress)
                  }
                  navigator.pop()
                } catch (_: CancellationException) {
                  offset.animateTo(0f)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun rememberSlideOverNavState(
  showInDetailPane: (Screen) -> Boolean,
  delegate: Navigator,
): SlideOverNavState {
  // todo saveable
  return remember(delegate) { SlideOverNavState(showInDetailPane, delegate) }
    .apply { this.showInDetailPane = showInDetailPane }
}

class SlideOverNavState(var showInDetailPane: (Screen) -> Boolean, val delegate: Navigator) :
  Navigator by delegate {

  var isDecorating by mutableStateOf(false)
    internal set

  var isActive by mutableStateOf(false)
    internal set

  private val isOffScreen = mutableStateMapOf<Screen, Boolean>()

  fun isOffScreen(screen: Screen?): Boolean {
    return screen != null && isOffScreen[screen] == true
  }

  fun setOnScreen(screen: Screen?) {
    isOffScreen.remove(screen)
  }

  override fun goTo(screen: Screen): Boolean {
    val peekBackStack = peekBackStack()
    val topScreen = peekBackStack.first()
    return Snapshot.withMutableSnapshot {
      if (isDecorating && isOffScreen(topScreen)) {
        // Pop off the top one to replace it as we're not navigating from the detail screen
        delegate.pop()
      }
      isOffScreen.remove(topScreen)
      // Then go to the new screen
      delegate.goTo(screen)
    }
  }

  override fun pop(result: PopResult?): Screen? {
    val peekBackStack = peekBackStack()
    val topScreen = peekBackStack.first()
    val nextScreen = peekBackStack.getOrNull(1)
    if (isDecorating && nextScreen != null) {
      // Slide over the detail screen
      if (!isOffScreen(topScreen) && showInDetailPane(topScreen) && !showInDetailPane(nextScreen)) {
        isOffScreen[topScreen] = true
        return null
      }
      setOnScreen(nextScreen)
    }
    setOnScreen(topScreen)
    return delegate.pop(result)
  }

  override fun resetRoot(newRoot: Screen, options: StateOptions): List<Screen> {
    setOnScreen(newRoot)
    // todo restore is open state here?
    return delegate.resetRoot(newRoot, options)
  }
}

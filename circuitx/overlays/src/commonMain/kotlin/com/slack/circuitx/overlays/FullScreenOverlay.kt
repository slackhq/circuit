// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.PredictiveBackHandler
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.overlay.AnimatedOverlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.overlay.OverlayTransitionController
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CancellationException

/**
 * Shows a full screen overlay with the given [screen]. As the name suggests, this overlay takes
 * over the entire available screen space available to the current [OverlayHost].
 */
public expect suspend fun OverlayHost.showFullScreenOverlay(screen: Screen): PopResult?

/**
 * A simple overlay that renders a given [screen] in a [CircuitContent] with backhandling support
 * and callbacks to [callbacks].
 */
internal class FullScreenOverlay<S : Screen>(
  private val screen: S,
  private val callbacks: @Composable () -> Callbacks = { Callbacks.NoOp },
) : AnimatedOverlay<FullScreenOverlay.Result>(fadeIn(), fadeOut()) {
  /** Simple callbacks for when a [FullScreenOverlay] is shown and finished. */
  @Stable
  internal interface Callbacks {
    fun onShow()

    fun onFinish()

    object NoOp : Callbacks {
      override fun onShow() {}

      override fun onFinish() {}
    }
  }

  @JvmInline internal value class Result(val result: PopResult?)

  @OptIn(ExperimentalComposeUiApi::class) // For PredictiveBackHandler
  @Composable
  override fun AnimatedVisibilityScope.AnimatedContent(
    navigator: OverlayNavigator<Result>,
    transitionController: OverlayTransitionController,
  ) {
    val callbacks = key(callbacks) { callbacks() }
    val dispatchingNavigator = remember {
      DispatchingOverlayNavigator(screen, navigator, callbacks::onFinish)
    }
    PredictiveBackHandler(enabled = true) { progress ->
      try {
        progress.collect { transitionController.seek(it.progress) }
        dispatchingNavigator.pop()
      } catch (_: CancellationException) {
        transitionController.cancel()
      }
    }
    CircuitContent(screen = screen, onNavEvent = dispatchingNavigator::onNavEvent)
  }
}

/**
 * A [Navigator] that dispatches to the given [overlayNavigator] and calls [onPop] when [pop] is
 * called.
 */
internal class DispatchingOverlayNavigator(
  private val currentScreen: Screen,
  private val overlayNavigator: OverlayNavigator<FullScreenOverlay.Result>,
  private val onPop: () -> Unit,
) : Navigator {
  override fun goTo(screen: Screen): Boolean {
    error("goTo() is not supported in full screen overlays!")
  }

  override fun forward(): Boolean {
    error("forward() is not supported in full screen overlays!")
  }

  override fun backward(): Boolean {
    error("backward() is not supported in full screen overlays!")
  }

  override fun pop(result: PopResult?): Screen? {
    overlayNavigator.finish(FullScreenOverlay.Result(result))
    onPop()
    return null
  }

  override fun peek(): Screen = currentScreen

  override fun peekBackStack(): List<Screen> = listOf(currentScreen)

  override fun peekNavStack(): NavStackList<Screen> = navStackListOf(currentScreen)

  override fun resetRoot(newRoot: Screen, options: Navigator.StateOptions): List<Screen> {
    error("resetRoot() is not supported in full screen overlays!")
  }
}

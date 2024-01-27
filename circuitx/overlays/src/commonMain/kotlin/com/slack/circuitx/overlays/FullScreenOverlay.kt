// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

/**
 * Shows a full screen overlay with the given [screen]. As the name suggests, this overlay takes
 * over the entire available screen space available to the current [OverlayHost].
 */
public expect suspend fun OverlayHost.showFullScreenOverlay(screen: Screen)

/**
 * A simple overlay that renders a given [screen] in a [CircuitContent] with backhandling support
 * and callbacks to [callbacks].
 */
internal class FullScreenOverlay<S : Screen>(
  private val screen: S,
  private val callbacks: @Composable () -> Callbacks = { Callbacks.NoOp },
) : Overlay<Unit> {
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

  @Composable
  override fun Content(navigator: OverlayNavigator<Unit>) {
    val callbacks = key(callbacks) { callbacks() }
    val dispatchingNavigator = remember {
      DispatchingOverlayNavigator(navigator, callbacks::onFinish)
    }

    BackHandler(enabled = true, onBack = dispatchingNavigator::pop)
    CircuitContent(screen = screen, onNavEvent = dispatchingNavigator::onNavEvent)
  }
}

/**
 * A [Navigator] that dispatches to the given [overlayNavigator] and calls [onPop] when [pop] is
 * called.
 */
internal class DispatchingOverlayNavigator(
  private val overlayNavigator: OverlayNavigator<Unit>,
  private val onPop: () -> Unit,
) : Navigator {
  override fun goTo(screen: Screen) {
    error("goTo() is not supported in full screen overlays!")
  }

  override fun pop(result: PopResult?): Screen? {
    overlayNavigator.finish(Unit)
    onPop()
    return null
  }

  override fun resetRoot(newRoot: Screen): List<Screen> {
    error("resetRoot() is not supported in full screen overlays!")
  }
}

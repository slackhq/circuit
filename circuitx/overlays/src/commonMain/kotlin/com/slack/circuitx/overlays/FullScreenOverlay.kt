// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.internal.BackHandler
import com.slack.circuit.foundation.onNavEvent
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen

public expect suspend fun OverlayHost.showFullScreenOverlay(screen: Screen)

@Stable
internal interface ShowSaveCallbacks {
  fun save()

  fun restore()

  object NoOp : ShowSaveCallbacks {
    override fun save() {}

    override fun restore() {}
  }
}

internal class FullScreenOverlay<S : Screen>(
  private val screen: S,
  private val showSaveCallbacks: @Composable () -> ShowSaveCallbacks = { ShowSaveCallbacks.NoOp },
) : Overlay<Unit> {

  @Composable
  override fun Content(navigator: OverlayNavigator<Unit>) {
    val callbacks = showSaveCallbacks()
    val dispatchingNavigator = remember {
      DispatchingOverlayNavigator(navigator) { callbacks.restore() }
    }

    BackHandler(enabled = true, onBack = dispatchingNavigator::pop)
    CircuitContent(screen = screen, onNavEvent = dispatchingNavigator::onNavEvent)
  }
}

internal class DispatchingOverlayNavigator(
  private val navigator: OverlayNavigator<Unit>,
  private val onPop: () -> Unit,
) : Navigator {
  override fun goTo(screen: Screen) {
    error("goTo() is not supported in full screen overlays!")
  }

  override fun pop(): Screen? {
    navigator.finish(Unit)
    onPop()
    return null
  }

  override fun resetRoot(newRoot: Screen): List<Screen> {
    error("resetRoot() is not supported in full screen overlays!")
  }
}

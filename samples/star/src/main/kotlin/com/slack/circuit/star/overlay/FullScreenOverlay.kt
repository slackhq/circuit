// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.overlay

import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.star.ui.ConditionalSystemUiColors

private class DispatchingOverlayNavigator(
  private val navigator: OverlayNavigator<Unit>,
  private val conditionalSystemUiColors: ConditionalSystemUiColors,
) : Navigator {
  override fun goTo(screen: Screen) {
    error("goTo() is not supported in full screen overlays!")
  }

  override fun pop(): Screen? {
    navigator.finish(Unit)
    conditionalSystemUiColors.restore()
    return null
  }

  override fun resetRoot(newRoot: Screen): List<Screen> {
    error("resetRoot() is not supported in full screen overlays!")
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen

public actual suspend fun OverlayHost.showFullScreenOverlay(screen: Screen): PopResult? {
  return show(FullScreenOverlay(screen)).result
}

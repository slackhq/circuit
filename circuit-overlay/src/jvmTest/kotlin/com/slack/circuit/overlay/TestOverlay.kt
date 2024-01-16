// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TestOverlay : Overlay<String> {
  private var result by mutableStateOf<String?>(null)

  fun finish(result: String) {
    this.result = result
  }

  @Composable
  override fun Content(navigator: OverlayNavigator<String>) {
    result?.let(navigator::finish)
  }
}

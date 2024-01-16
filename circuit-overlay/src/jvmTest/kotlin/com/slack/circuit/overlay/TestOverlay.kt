package com.slack.circuit.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

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
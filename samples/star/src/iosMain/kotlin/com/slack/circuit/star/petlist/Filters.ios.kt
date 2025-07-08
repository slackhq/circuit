package com.slack.circuit.star.petlist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay

actual suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters {
  // TODO show in a native bottom sheet using Calf
  return show(
    BottomSheetOverlay(model = currentFilters, onDismiss = { currentFilters }) {
        initialFilters,
        overlayNavigator ->
      Surface(Modifier.fillMaxWidth()) {
        UpdateFiltersSheet(
          initialFilters,
          Modifier.padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
          overlayNavigator::finish,
        )
      }
    }
  )
}
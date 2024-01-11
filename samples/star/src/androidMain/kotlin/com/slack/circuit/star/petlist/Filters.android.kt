// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay

/** Indirection for showing filters in the pet list screen. */
actual suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters {
  return show(
    BottomSheetOverlay(
      model = currentFilters,
      onDismiss = { currentFilters },
    ) { initialFilters, overlayNavigator ->
      Surface(Modifier.fillMaxWidth()) {
        UpdateFiltersSheet(
          initialFilters,
          Modifier.padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
          overlayNavigator::finish
        )
      }
    }
  )
}

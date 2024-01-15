// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuitx.overlays.AlertDialogOverlay
import com.slack.circuitx.overlays.DialogOverlay

/** Indirection for showing filters in the pet list screen. */
actual suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters {
  return show(
    AlertDialogOverlay(
      model = currentFilters,
      onDismiss = { currentFilters },
    ) { initialFilters, overlayNavigator ->
      UpdateFiltersSheet(initialFilters, Modifier.padding(32.dp), overlayNavigator::finish)
    }
  )
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BasicAlertDialogOverlay

/** Indirection for showing filters in the pet list screen. */
actual suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters {
  return show(
    BasicAlertDialogOverlay(model = currentFilters, onDismissRequest = { currentFilters }) {
      initialFilters,
      overlayNavigator ->
      UpdateFiltersSheet(initialFilters, Modifier.padding(32.dp), overlayNavigator::finish)
    }
  )
}

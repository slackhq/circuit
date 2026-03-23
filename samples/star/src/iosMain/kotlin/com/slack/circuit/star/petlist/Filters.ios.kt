// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.petlist

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay

actual suspend fun OverlayHost.updateFilters(
  currentFilters: Filters,
  sheetBackgroundColor: Color,
): Filters {
  return show(
    BottomSheetOverlay(
      model = currentFilters,
      onDismiss = { currentFilters },
      sheetContainerColor = sheetBackgroundColor.takeOrNull(),
    ) { initialFilters, overlayNavigator ->
      UpdateFiltersSheet(
        initialFilters,
        Modifier.padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
        overlayNavigator::finish,
      )
    }
  )
}

private fun Color.takeOrNull(): Color? = if (this == Color.Unspecified) null else this

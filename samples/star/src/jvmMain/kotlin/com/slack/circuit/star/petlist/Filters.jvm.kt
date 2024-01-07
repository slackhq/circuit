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

/** Indirection for showing filters in the pet list screen. */
actual suspend fun OverlayHost.updateFilters(currentFilters: Filters): Filters {
  return show(
    DialogOverlay(
      model = currentFilters,
      onDismiss = { currentFilters },
    ) { initialFilters, overlayNavigator ->
      UpdateFiltersSheet(initialFilters, Modifier.padding(32.dp), overlayNavigator::finish)
    }
  )
}

// TODO maybe move this to circuitx-overlay
class DialogOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val onDismiss: (() -> Result)? = null,
  private val properties: DialogProperties = DialogProperties(),
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {
  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    Dialog(
      content = {
        Surface(
          shape = AlertDialogDefaults.shape,
          color = AlertDialogDefaults.containerColor,
          tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
          content(model, navigator::finish)
        }
      },
      properties = properties,
      onDismissRequest = {
        // This is apparently as close as we can get to an "onDismiss" callback, which
        // unfortunately has no animation
        navigator.finish(onDismiss!!.invoke())
      },
    )
  }
}

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator

/** An overlay that shows a [Dialog]. */
public class BasicDialogOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val onDismissRequest: () -> Result,
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
        navigator.finish(onDismissRequest())
      },
    )
  }
}

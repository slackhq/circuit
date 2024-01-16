// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuitx.overlays.DialogResult.Cancel
import com.slack.circuitx.overlays.DialogResult.Confirm
import com.slack.circuitx.overlays.DialogResult.Dismiss

/**
 * An overlay that shows an [AlertDialog] with configurable inputs.
 *
 * @see AlertDialog for docs on the parameters
 */
public class AlertDialogOverlay(
  private val confirmButtonText: @Composable () -> Unit,
  private val icon: @Composable (() -> Unit)? = null,
  private val title: @Composable (() -> Unit)? = null,
  private val text: @Composable (() -> Unit)? = null,
  private val dismissButtonText: (@Composable () -> Unit)?,
  private val properties: DialogProperties = DialogProperties(),
) : Overlay<DialogResult> {
  @Composable
  override fun Content(navigator: OverlayNavigator<DialogResult>) {
    AlertDialog(
      onDismissRequest = { navigator.finish(Dismiss) },
      icon = icon,
      title = title,
      text = text,
      confirmButton = { Button(onClick = { navigator.finish(Confirm) }) { confirmButtonText() } },
      dismissButton =
        dismissButtonText?.let { dismissButtonText ->
          { Button(onClick = { navigator.finish(Cancel) }) { dismissButtonText() } }
        },
      properties = properties,
    )
  }
}

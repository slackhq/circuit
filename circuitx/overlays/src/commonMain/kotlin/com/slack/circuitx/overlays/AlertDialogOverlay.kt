// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.slack.circuitx.overlays.DialogResult.Cancel
import com.slack.circuitx.overlays.DialogResult.Confirm
import com.slack.circuitx.overlays.DialogResult.Dismiss

public typealias OnClick = () -> Unit

/**
 * An overlay that shows an [AlertDialog] with configurable inputs.
 *
 * @see AlertDialog for docs on the parameters
 */
@ExperimentalMaterial3Api
public fun alertDialogOverlay(
  confirmButton: @Composable (onClick: OnClick) -> Unit,
  icon: @Composable (() -> Unit)? = null,
  title: @Composable (() -> Unit)? = null,
  text: @Composable (() -> Unit)? = null,
  dismissButton: (@Composable (onClick: OnClick) -> Unit)?,
  properties: DialogProperties = DialogProperties(),
): BasicAlertDialogOverlay<*, DialogResult> {
  return BasicAlertDialogOverlay(
    model = Unit,
    onDismissRequest = { Dismiss },
    properties = properties,
  ) { _, navigator ->
    AlertDialog(
      onDismissRequest = { navigator.finish(Dismiss) },
      icon = icon,
      title = title,
      text = text,
      confirmButton = { confirmButton { navigator.finish(Confirm) } },
      dismissButton =
        dismissButton?.let { dismissButton -> { dismissButton { navigator.finish(Cancel) } } },
      properties = properties,
    )
  }
}

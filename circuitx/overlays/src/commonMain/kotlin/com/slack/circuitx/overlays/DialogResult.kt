// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

/** An enum representing the possible results of an [alertDialogOverlay]. */
public enum class DialogResult {
  /** The user confirmed the action. */
  Confirm,

  /** The user canceled the action (i.e. clicked the dismiss button). */
  Cancel,

  /** The user dismissed the dialog (i.e. clicked outside of the dialog). */
  Dismiss,
}

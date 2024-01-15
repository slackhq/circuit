// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.overlays

public sealed interface DialogResult {
  public data object Confirm : DialogResult

  public data object Cancel : DialogResult

  public data object Dismiss : DialogResult
}

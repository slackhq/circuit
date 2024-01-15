package com.slack.circuitx.overlays

public sealed interface DialogResult {
  public data object Confirm : DialogResult

  public data object Cancel : DialogResult

  public data object Dismiss : DialogResult
}
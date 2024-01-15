package com.slack.circuit.overlay

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.slack.circuit.overlay.OverlayState.UNAVAILABLE


/** A [ProvidableCompositionLocal] to expose the current [OverlayState] in the composition tree. */
public val LocalOverlayState: ProvidableCompositionLocal<OverlayState> = compositionLocalOf {
  UNAVAILABLE
}

/** Represents the current state of the available [OverlayHost]. */
public enum class OverlayState {
  /**
   * Indicates that overlays are not available.
   */
  UNAVAILABLE,

  /**
   * Indicates that overlays are available but no overlay is currently showing.
   */
  HIDDEN,

  /**
   * Indicates that an overlay is currently showing.
   */
  SHOWING
}
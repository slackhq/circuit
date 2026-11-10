// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList

/**
 * Decides when a [PaneNavDecoration] shows two panes, and which record takes the left pane.
 *
 * A strategy only maps the window and the stack to a [PaneScene]. The decoration owns the layout,
 * insets, record lifecycle, animation scopes, and [PaneRole]s.
 *
 * [PaneNavDecoration] calls [scene] during composition. An implementation must return equal scenes
 * for equal inputs and must not read state that changes during measure. Shared element transitions
 * add a lookahead pass, and both passes need the same pane widths.
 */
@ExperimentalCircuitAdaptiveApi
@Stable
public interface PaneStrategy {
  /** Returns the panes for [stack] in [window], or null to show only the top record. */
  public fun scene(window: PaneWindow, stack: NavStackList<out NavArgument>): PaneScene?
}

/** The window that a [PaneNavDecoration] fills. */
@ExperimentalCircuitAdaptiveApi
@Immutable
public class PaneWindow(
  /** The size of the decoration. */
  public val size: DpSize,
  /** The window size class of [size]. */
  public val sizeClass: WindowSizeClass,
  /**
   * Vertical hinges that separate the display, in window coordinates. Empty on iOS and desktop.
   *
   * The decoration must fill the window for these bounds to line up with its panes.
   */
  public val separatingHinges: List<DpRect>,
) {
  override fun equals(other: Any?): Boolean {
    return other is PaneWindow &&
      size == other.size &&
      sizeClass == other.sizeClass &&
      separatingHinges == other.separatingHinges
  }

  override fun hashCode(): Int {
    var result = size.hashCode()
    result = 31 * result + sizeClass.hashCode()
    result = 31 * result + separatingHinges.hashCode()
    return result
  }

  override fun toString(): String {
    return "PaneWindow(size=$size, sizeClass=$sizeClass, separatingHinges=$separatingHinges)"
  }
}

/**
 * Two panes over one stack. The record with [primaryKey] takes the left pane. Every record above it
 * goes to the right pane, and records below it stay hidden.
 */
@ExperimentalCircuitAdaptiveApi
@Immutable
public class PaneScene(
  /** Key of the record in the left pane. It renders with no transitions. */
  public val primaryKey: String,
  /**
   * Width of the left pane, or null to split at the first hinge of the window. The decoration adds
   * the start window inset to this width.
   */
  public val primaryWidth: Dp?,
  /**
   * Draws the right pane while no record is open there, or null to give the left pane the full
   * width. [PaneHost.placeholder] takes priority over this.
   */
  public val placeholder: (@Composable () -> Unit)? = null,
) {
  override fun equals(other: Any?): Boolean {
    return other is PaneScene &&
      primaryKey == other.primaryKey &&
      primaryWidth == other.primaryWidth &&
      placeholder == other.placeholder
  }

  override fun hashCode(): Int {
    var result = primaryKey.hashCode()
    result = 31 * result + primaryWidth.hashCode()
    result = 31 * result + placeholder.hashCode()
    return result
  }

  override fun toString(): String {
    return "PaneScene(primaryKey=$primaryKey, primaryWidth=$primaryWidth)"
  }
}

/**
 * Creates a [PaneWindow] of [size] with Material's window size class breakpoints and the hinges
 * that Material adaptive reports for the current window.
 */
@ExperimentalCircuitAdaptiveApi
@Composable
public fun rememberPaneWindow(size: DpSize): PaneWindow {
  val density = LocalDensity.current
  val hinges =
    currentWindowAdaptiveInfoV2().windowPosture.separatingVerticalHingeBounds.map { bounds ->
      with(density) {
        DpRect(bounds.left.toDp(), bounds.top.toDp(), bounds.right.toDp(), bounds.bottom.toDp())
      }
    }
  return PaneWindow(size, hinges)
}

/** Creates a [PaneWindow] of [size] with Material's window size class breakpoints. */
@ExperimentalCircuitAdaptiveApi
public fun PaneWindow(size: DpSize, separatingHinges: List<DpRect> = emptyList()): PaneWindow {
  val sizeClass =
    WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(
      widthDp = size.width.value,
      heightDp = size.height.value,
    )
  return PaneWindow(size, sizeClass, separatingHinges)
}

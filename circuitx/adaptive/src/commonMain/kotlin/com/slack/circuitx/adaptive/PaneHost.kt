// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.slack.circuit.runtime.screen.Screen

/** Where a record renders. Screens read it to adapt back buttons and shared elements. */
@ExperimentalCircuitAdaptiveApi
public enum class PaneRole {
  /** The only visible screen, as on phones. */
  SINGLE,

  /** The screen in the left pane. */
  PRIMARY,

  /** The bottom screen of the right pane, opened from the primary screen. */
  DETAIL_ROOT,

  /** A screen pushed over another screen in the right pane. */
  DETAIL_STACKED,
}

/**
 * State that a [PaneNavDecoration] shares with the screens it shows and with
 * [PaneReplaceInterceptor].
 *
 * The decoration writes the layout it chose after each composition. Screens read it from
 * [LocalPaneHost], and the primary screen can set [placeholder] to reserve the right pane.
 */
@ExperimentalCircuitAdaptiveApi
@Stable
public class PaneHost {
  /** The scene from the last composition, or null while one record fills the window. */
  public var scene: PaneScene? by mutableStateOf(null)
    internal set

  /** The screen at the bottom of the right pane. Lists use it to highlight the open item. */
  public var detailRoot: Screen? by mutableStateOf(null)
    internal set

  /**
   * Reserves the right pane and draws it while no record is open there. It takes priority over
   * [PaneScene.placeholder].
   *
   * A screen that switches content, like a tab host, should set this in the same event that
   * switches the content. The next frame then lays out at the final widths.
   */
  public var placeholder: (@Composable () -> Unit)? by mutableStateOf(null)

  /** The window shows two panes. */
  public val isSplit: Boolean
    get() = scene != null

  /**
   * How many records the primary record and the records below it make up. The count does not change
   * when records above the primary record change, so it stays correct between compositions.
   */
  internal var primaryDepth: Int by mutableIntStateOf(0)
}

/** The [PaneHost] of the nearest [PaneNavDecoration], or null outside one. */
@ExperimentalCircuitAdaptiveApi
public val LocalPaneHost: ProvidableCompositionLocal<PaneHost?> = staticCompositionLocalOf { null }

/** The [PaneRole] of the current record. It is [PaneRole.SINGLE] outside a [PaneNavDecoration]. */
@ExperimentalCircuitAdaptiveApi
public val LocalPaneRole: ProvidableCompositionLocal<PaneRole> = compositionLocalOf {
  PaneRole.SINGLE
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.slack.circuit.runtime.screen.Screen

/** Where a record renders. Screens read it to adapt back buttons and shared elements. */
@ExperimentalCircuitAdaptiveApi
public enum class PaneRole {
  /** The only visible screen, as on phones. */
  SINGLE,

  /**
   * The screen in the left pane. It has the full width while nothing is open beside it and no
   * placeholder is set. Read [PaneState.isSplit] to tell the two apart.
   */
  PRIMARY,

  /**
   * The screen opened from the primary screen. It is the bottom screen of the right pane with two
   * panes, and it takes the middle pane with three.
   */
  DETAIL_ROOT,

  /**
   * A screen pushed over the detail. It is in the right pane with two panes, and in the middle pane
   * or hidden below it with three.
   */
  DETAIL_STACKED,

  /** The bottom screen of the third pane, opened from a detail. */
  EXTRA_ROOT,

  /** A screen pushed over another screen in the third pane. */
  EXTRA_STACKED,
}

/**
 * The layout that a [PaneNavDecoration] chose for the composition that reads it.
 *
 * Screens read it from [LocalPaneState] to draw for the layout they are in, like a tab host that
 * shows a navigation rail while the window is split. The value is ready in the first frame and in
 * the same frame as a window resize.
 */
@ExperimentalCircuitAdaptiveApi
@Immutable
public class PaneState(
  /** The scene of this composition, or null while one record fills the window. */
  public val scene: PaneScene?,
  /** The screen at the bottom of the right pane. Lists use it to highlight the open item. */
  public val detailRoot: Screen?,
  /**
   * The decoration measures the left pane with a minimum width of zero and uses the width its
   * content asks for. It is true for a [PaneSplit.PrimaryWraps] scene while the right pane shows.
   * The screen in the left pane must then choose its own width.
   */
  public val primarySizesItself: Boolean = false,
  /**
   * The width for a pane that does not take the remaining width, from [PaneWindow]. A left pane
   * that sizes itself can give it to its list. It is [Dp.Unspecified] outside a decoration.
   */
  public val preferredPaneWidth: Dp = Dp.Unspecified,
  /**
   * How many panes the window shows now. It is 1 for a [scene] whose left pane has the full width,
   * because nothing is open beside it and no placeholder is set.
   */
  public val paneCount: Int = 1,
) {
  /** The window shows more than one pane. */
  public val isSplit: Boolean
    get() = paneCount > 1

  override fun equals(other: Any?): Boolean {
    return other is PaneState &&
      scene == other.scene &&
      detailRoot == other.detailRoot &&
      primarySizesItself == other.primarySizesItself &&
      preferredPaneWidth == other.preferredPaneWidth &&
      paneCount == other.paneCount
  }

  override fun hashCode(): Int {
    var result = scene.hashCode()
    result = 31 * result + detailRoot.hashCode()
    result = 31 * result + primarySizesItself.hashCode()
    result = 31 * result + preferredPaneWidth.hashCode()
    result = 31 * result + paneCount
    return result
  }

  override fun toString(): String {
    return "PaneState(scene=$scene, detailRoot=$detailRoot, " +
      "primarySizesItself=$primarySizesItself, preferredPaneWidth=$preferredPaneWidth, " +
      "paneCount=$paneCount)"
  }

  public companion object {
    /** One record fills the window. */
    public val Single: PaneState = PaneState(scene = null, detailRoot = null)
  }
}

/**
 * The [PaneState] of the nearest [PaneNavDecoration]. It is [PaneState.Single] outside one.
 *
 * Code outside composition, like a [PaneReplaceInterceptor], reads [PaneHost] for the same values.
 */
@ExperimentalCircuitAdaptiveApi
public val LocalPaneState: ProvidableCompositionLocal<PaneState> = compositionLocalOf {
  PaneState.Single
}

/**
 * State that a [PaneNavDecoration] shares with [PaneReplaceInterceptor] and with the screens it
 * shows.
 *
 * The decoration writes the layout it chose after each composition, so [scene] and [detailRoot] are
 * one composition behind for a screen that reads them while it composes. Screens read
 * [LocalPaneState] for those. The primary screen sets [placeholder] here to reserve the right pane.
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
   * [PaneScene.placeholder]. Null uses the placeholder of the scene. [NoPlaceholder] gives the left
   * pane the full width, also when the scene has a placeholder.
   *
   * A screen that switches content, like a tab host, should set this in the same event that
   * switches the content. The next frame then lays out at the final widths.
   */
  public var placeholder: (@Composable () -> Unit)? by mutableStateOf(null)

  /**
   * How many panes the window showed in the last composition. It is 1 for a [scene] whose left pane
   * has the full width.
   */
  public var paneCount: Int by mutableIntStateOf(1)
    internal set

  /** The window shows more than one pane. */
  public val isSplit: Boolean
    get() = paneCount > 1

  /**
   * How many records the primary record and the records below it make up. The count does not change
   * when records above the primary record change, so it stays correct between compositions.
   */
  internal var primaryDepth: Int by mutableIntStateOf(0)

  public companion object {
    /**
     * A [placeholder] value that reserves nothing. Set it to give the left pane the full width
     * while no record is open beside it, in place of the placeholder of the scene.
     */
    public val NoPlaceholder: @Composable () -> Unit = {}
  }
}

/** The [PaneHost] of the nearest [PaneNavDecoration], or null outside one. */
@ExperimentalCircuitAdaptiveApi
public val LocalPaneHost: ProvidableCompositionLocal<PaneHost?> = staticCompositionLocalOf { null }

/** The [PaneRole] of the current record. It is [PaneRole.SINGLE] outside a [PaneNavDecoration]. */
@ExperimentalCircuitAdaptiveApi
public val LocalPaneRole: ProvidableCompositionLocal<PaneRole> = compositionLocalOf {
  PaneRole.SINGLE
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
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
 * Decides when a [PaneNavDecoration] shows more than one pane, and which records take the panes.
 *
 * A strategy only maps the window and the stack to a [PaneScene]. The decoration owns the layout,
 * insets, record lifecycle, animation scopes, and [PaneRole]s.
 *
 * [PaneNavDecoration] calls [scene] during composition. An implementation must return equal scenes
 * for equal inputs and must not read state that changes during measure.
 */
@ExperimentalCircuitAdaptiveApi
@Stable
public interface PaneStrategy {
  /** Returns the panes for [stack] in [window], or null to show only the top record. */
  public fun scene(window: PaneWindow, stack: NavStackList<out NavArgument>): PaneScene?
}

/**
 * The window that a [PaneNavDecoration] fills.
 *
 * [maxPanes] and [preferredPaneWidth] come from Material's pane guidance when [rememberPaneWindow]
 * or the [PaneWindow] function creates the window. Pass other values to change when the layout
 * splits and how wide its panes are.
 */
@ExperimentalCircuitAdaptiveApi
@Immutable
public class PaneWindow(
  /** The size of the decoration. */
  public val size: DpSize,
  /** The window size class of [size]. */
  public val sizeClass: WindowSizeClass,
  /**
   * Vertical hinges that separate the display. Empty on iOS and desktop.
   *
   * A `window` function of a [PaneNavDecoration] returns them in window coordinates. The decoration
   * moves them to its own coordinates before it calls a strategy: `left` is then the distance from
   * the start edge of the decoration to the hinge, and `right` is the distance to the far side of
   * the hinge, in both layout directions. A strategy can compare them with [size].
   */
  public val separatingHinges: List<DpRect>,
  /** How many panes fit side by side in this window. */
  public val maxPanes: Int,
  /** The width for a pane that does not take the remaining width. */
  public val preferredPaneWidth: Dp,
) {
  override fun equals(other: Any?): Boolean {
    return other is PaneWindow &&
      size == other.size &&
      sizeClass == other.sizeClass &&
      separatingHinges == other.separatingHinges &&
      maxPanes == other.maxPanes &&
      preferredPaneWidth == other.preferredPaneWidth
  }

  override fun hashCode(): Int {
    var result = size.hashCode()
    result = 31 * result + sizeClass.hashCode()
    result = 31 * result + separatingHinges.hashCode()
    result = 31 * result + maxPanes
    result = 31 * result + preferredPaneWidth.hashCode()
    return result
  }

  override fun toString(): String {
    return "PaneWindow(size=$size, sizeClass=$sizeClass, separatingHinges=$separatingHinges, " +
      "maxPanes=$maxPanes, preferredPaneWidth=$preferredPaneWidth)"
  }
}

/**
 * How a [PaneNavDecoration] finds the edge between the left pane of a [PaneScene] and the pane
 * beside it.
 *
 * A strategy picks a split in composition. The decoration resolves it to a width when it measures,
 * with the same result in the lookahead pass and the main pass.
 */
@ExperimentalCircuitAdaptiveApi
@Immutable
public sealed interface PaneSplit {
  /**
   * The left pane is [primaryWidth] wide and the pane beside it takes the rest. The decoration adds
   * the start window inset to [primaryWidth].
   */
  public class Fixed(public val primaryWidth: Dp) : PaneSplit {
    override fun equals(other: Any?): Boolean {
      return other is Fixed && primaryWidth == other.primaryWidth
    }

    override fun hashCode(): Int {
      return primaryWidth.hashCode()
    }

    override fun toString(): String {
      return "PaneSplit.Fixed(primaryWidth=$primaryWidth)"
    }
  }

  /**
   * The pane beside the left pane is [rightWidth] wide and the left pane takes the rest. The
   * decoration adds the end window inset to [rightWidth]. Use it for a panel beside a main screen.
   *
   * The decoration ignores [PaneScene.secondaryKey] with this split.
   */
  public class RightFixed(public val rightWidth: Dp) : PaneSplit {
    override fun equals(other: Any?): Boolean {
      return other is RightFixed && rightWidth == other.rightWidth
    }

    override fun hashCode(): Int {
      return rightWidth.hashCode()
    }

    override fun toString(): String {
      return "PaneSplit.RightFixed(rightWidth=$rightWidth)"
    }
  }

  /**
   * The left pane takes [primaryFraction] of the width, from 0 to 1, and the pane beside it takes
   * the rest.
   */
  public class Fraction(public val primaryFraction: Float) : PaneSplit {
    init {
      require(primaryFraction in 0f..1f) { "primaryFraction must be from 0 to 1" }
    }

    override fun equals(other: Any?): Boolean {
      return other is Fraction && primaryFraction == other.primaryFraction
    }

    override fun hashCode(): Int {
      return primaryFraction.hashCode()
    }

    override fun toString(): String {
      return "PaneSplit.Fraction(primaryFraction=$primaryFraction)"
    }
  }

  /**
   * The panes split at the first hinge of the window, or in the middle when it has none.
   *
   * The decoration ignores [PaneScene.secondaryKey] with this split, so a hinge never crosses a
   * pane.
   */
  public data object AtHinge : PaneSplit

  /**
   * The left pane takes the width that its content asks for, up to [maxWidth], and the pane beside
   * it takes the rest. Use it when the left pane holds more than one sized part.
   *
   * The decoration measures the left pane with a minimum width of zero. Its screen must then choose
   * a width, because content that fills the maximum width leaves no room beside it. The screen
   * reads [PaneState.primarySizesItself] to know when this applies, and can use
   * [PaneState.preferredPaneWidth] for a list. The screen also handles the start window inset.
   *
   * @param maxWidth The widest the left pane can be, or [Dp.Unspecified] for the window width.
   */
  public class PrimaryWraps(public val maxWidth: Dp = Dp.Unspecified) : PaneSplit {
    override fun equals(other: Any?): Boolean {
      return other is PrimaryWraps && maxWidth == other.maxWidth
    }

    override fun hashCode(): Int {
      return maxWidth.hashCode()
    }

    override fun toString(): String {
      return "PaneSplit.PrimaryWraps(maxWidth=$maxWidth)"
    }
  }
}

/**
 * Two or three panes over one stack.
 *
 * With two panes, the record with [primaryKey] takes the left pane and every record above it goes
 * to the right pane. Records below it stay hidden.
 *
 * With a [secondaryKey], its record takes a middle pane and only the records above that one go to
 * the right pane. Records between the two keys stay hidden.
 */
@ExperimentalCircuitAdaptiveApi
@Immutable
public class PaneScene(
  /** Key of the record in the left pane. It renders with no transitions. */
  public val primaryKey: String,
  /** How the decoration finds the edge between the left pane and the pane beside it. */
  public val split: PaneSplit,
  /**
   * Draws the right pane while no record is open there, or null to give the left pane the full
   * width. [PaneHost.placeholder] takes priority over this.
   */
  public val placeholder: (@Composable () -> Unit)? = null,
  /**
   * Key of the record in a middle pane, or null for two panes. The record must be above the one
   * with [primaryKey] and below the top of the stack. It renders with no transitions and takes the
   * width that the other panes leave.
   */
  public val secondaryKey: String? = null,
  /** Width of the right pane while [secondaryKey] is set. */
  public val rightPaneWidth: Dp = Dp.Unspecified,
) {
  override fun equals(other: Any?): Boolean {
    return other is PaneScene &&
      primaryKey == other.primaryKey &&
      split == other.split &&
      placeholder == other.placeholder &&
      secondaryKey == other.secondaryKey &&
      rightPaneWidth == other.rightPaneWidth
  }

  override fun hashCode(): Int {
    var result = primaryKey.hashCode()
    result = 31 * result + split.hashCode()
    result = 31 * result + placeholder.hashCode()
    result = 31 * result + secondaryKey.hashCode()
    result = 31 * result + rightPaneWidth.hashCode()
    return result
  }

  override fun toString(): String {
    return "PaneScene(primaryKey=$primaryKey, split=$split, secondaryKey=$secondaryKey, " +
      "rightPaneWidth=$rightPaneWidth)"
  }
}

/**
 * Creates a [PaneWindow] of [size] with Material's pane guidance for that size and the hinges that
 * Material adaptive reports for the current window.
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

/**
 * Creates a [PaneWindow] of [size] with Material's pane guidance for that size.
 *
 * [PaneWindow.maxPanes] and [PaneWindow.preferredPaneWidth] come from Material adaptive's pane
 * scaffold directive: one pane below the expanded width class, two from it, and three from the
 * large width class.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalCircuitAdaptiveApi
public fun PaneWindow(size: DpSize, separatingHinges: List<DpRect> = emptyList()): PaneWindow {
  val sizeClass =
    WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
      widthDp = size.width.value,
      heightDp = size.height.value,
    )
  // The directive reads only the size class here. The hinges stay in dp on the window.
  val directive = calculatePaneScaffoldDirective(WindowAdaptiveInfo(sizeClass, Posture()))
  return PaneWindow(
    size = size,
    sizeClass = sizeClass,
    separatingHinges = separatingHinges,
    maxPanes = directive.maxHorizontalPartitions,
    preferredPaneWidth = directive.defaultPanePreferredWidth,
  )
}

// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.navstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrDefault

/**
 * The space available to a nav stage or one of its panes, the pane-sized counterpart to
 * [WindowInfo]'s container size. Read it with [currentPaneWindowSize] or [currentPaneWindowDpSize]
 * for breakpoints that should follow the space a screen is given rather than the whole window.
 *
 * Sizes come from the constraints the space is measured with, so they're current for the frame
 * being laid out. An unbounded dimension reports the enclosing pane's size, or the window's.
 */
@Stable
@ExperimentalNavStageApi
public interface PaneWindowInfo {
  /** The size of the space available, in pixels. */
  public val containerSize: IntSize

  /** The size of the space available, in [Dp][androidx.compose.ui.unit.Dp]. */
  public val containerDpSize: DpSize
}

/**
 * The [PaneWindowInfo] for the nearest enclosing pane, or nav stage outside of a pane. Null outside
 * of a [NavStageDecoration].
 */
@ExperimentalNavStageApi
public val LocalPaneWindowInfo: ProvidableCompositionLocal<PaneWindowInfo?> = compositionLocalOf {
  null
}

/** [LocalPaneWindowInfo]'s container size, or [LocalWindowInfo]'s outside of a nav stage. */
@ExperimentalNavStageApi
@Composable
public fun currentPaneWindowSize(): IntSize =
  LocalPaneWindowInfo.current?.containerSize ?: LocalWindowInfo.current.containerSize

/** [LocalPaneWindowInfo]'s container dp size, or [LocalWindowInfo]'s outside of a nav stage. */
@ExperimentalNavStageApi
@Composable
public fun currentPaneWindowDpSize(): DpSize =
  LocalPaneWindowInfo.current?.containerDpSize ?: LocalWindowInfo.current.containerDpSize

@OptIn(ExperimentalNavStageApi::class)
private data class MeasuredPaneWindowInfo(
  override val containerSize: IntSize,
  override val containerDpSize: DpSize,
) : PaneWindowInfo

/**
 * Lays out [content] as if it were placed directly with [modifier], and provides the space it's
 * given as [LocalPaneWindowInfo]. [content] is composed during measure, so it sees the size for the
 * frame it's laid out in.
 */
@OptIn(ExperimentalNavStageApi::class)
@Composable
internal fun ProvidePaneWindowInfo(modifier: Modifier, content: @Composable () -> Unit) {
  val enclosingSize = currentPaneWindowSize()
  SubcomposeLayout(modifier) { constraints ->
    val available =
      IntSize(
        if (constraints.hasBoundedWidth) constraints.maxWidth else enclosingSize.width,
        if (constraints.hasBoundedHeight) constraints.maxHeight else enclosingSize.height,
      )
    val info =
      MeasuredPaneWindowInfo(available, DpSize(available.width.toDp(), available.height.toDp()))
    val placeables =
      subcompose(Unit) {
          CompositionLocalProvider(LocalPaneWindowInfo provides info, content = content)
        }
        .fastMap { it.measure(constraints) }
    val width =
      placeables
        .fastMaxOfOrDefault(constraints.minWidth) { it.width }
        .coerceAtMost(constraints.maxWidth)
    val height =
      placeables
        .fastMaxOfOrDefault(constraints.minHeight) { it.height }
        .coerceAtMost(constraints.maxHeight)
    layout(width, height) { placeables.fastForEach { it.place(0, 0) } }
  }
}

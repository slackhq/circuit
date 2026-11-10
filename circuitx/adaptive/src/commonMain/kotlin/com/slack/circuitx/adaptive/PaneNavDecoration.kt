// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import com.slack.circuit.foundation.LocalRecordLifecycle
import com.slack.circuit.foundation.NavDecoration
import com.slack.circuit.foundation.ProvideRecordLifecycle
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.navigation.transform
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.ProvideStaticAnimatedTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation

/**
 * Shows one record beside the records above it when [strategy] returns a [PaneScene], and passes
 * the whole stack to [delegate] otherwise.
 *
 * Circuit keeps one back stack in both layouts. A fold or unfold only changes how the stack
 * renders, so records never move between stacks and keep their retained and saved state.
 *
 * In the split layout:
 * - The left pane shows the primary record of the scene.
 * - The right pane passes the records above the primary record to [delegate], with a placeholder
 *   below them. Back gestures and their animations act on the right pane only, and going back from
 *   the first detail reveals the placeholder, like the secondary column of an iPad split view.
 *
 * The right pane always renders through the same [delegate] call, so a layout switch never remounts
 * the open detail. Circuit wraps each record's content in movable content, so the primary record
 * keeps its state when it moves between the left pane and [delegate]. Each record renders in one
 * place only, because two calls would reuse the same saved state keys.
 *
 * Every layout decision is made in composition, never read during measure. Shared element
 * transitions add a lookahead pass, and both passes must agree on the pane widths.
 *
 * @param strategy Decides when to split and which record takes the left pane.
 * @param delegate Renders the single layout, and the right pane of the split layout.
 * @param host Receives the layout this decoration chose. Share it with [PaneReplaceInterceptor].
 * @param window Creates the [PaneWindow] for the size of this decoration. Override it to supply
 *   hinges from another source.
 * @param divider Drawn between the panes, at its intrinsic width.
 */
@ExperimentalCircuitAdaptiveApi
@Stable
public class PaneNavDecoration(
  private val strategy: PaneStrategy,
  private val delegate: NavDecoration,
  private val host: PaneHost = PaneHost(),
  private val window: @Composable (DpSize) -> PaneWindow = { rememberPaneWindow(it) },
  private val divider: @Composable () -> Unit = {},
) : NavDecoration {

  @OptIn(ExperimentalSharedTransitionApi::class)
  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    BoxWithConstraints(modifier) {
      // Top first, root last. Forward items stay hidden, as Circuit shows the active record.
      val visible = remember(args) { listOf(args.active) + args.backwardItems }
      val paneWindow = window(DpSize(maxWidth, maxHeight))
      val panes =
        remember(args, visible, paneWindow) {
          val scene = strategy.scene(paneWindow, args)
          val primaryIndex = scene?.let { visible.indexOfFirst { it.key == scene.primaryKey } }
          if (scene != null && primaryIndex != null && primaryIndex >= 0) {
            Panes(scene, visible[primaryIndex], visible.subList(0, primaryIndex))
          } else {
            null
          }
        }
      val isSplit = panes != null
      SideEffect {
        host.scene = panes?.scene
        host.detailRoot = panes?.abovePrimary?.lastOrNull()?.screen
        if (panes != null) {
          host.primaryDepth = visible.size - panes.abovePrimary.size
        }
      }
      val paneArgs =
        remember(args, panes) {
          if (panes != null) {
            rightPaneStack(args, panes.abovePrimary)
          } else {
            args.transform<T, PaneArg<T>> { PaneArg.Record(it, PaneRole.SINGLE) }
          }
        }

      val safeDrawing = WindowInsets.safeDrawing
      val startInset =
        safeDrawing
          .only(WindowInsetsSides.Start)
          .asPaddingValues()
          .calculateStartPadding(LocalLayoutDirection.current)

      val placeholder = host.placeholder ?: panes?.scene?.placeholder
      val showsDetail = panes != null && (panes.abovePrimary.isNotEmpty() || placeholder != null)
      // The hinge to split at. A scene without a width and without a hinge splits in the middle.
      val hinge = paneWindow.separatingHinges.firstOrNull()

      CompositionLocalProvider(LocalPaneHost provides host) {
        Layout(
          modifier = Modifier.fillMaxSize(),
          content = {
            if (panes != null) {
              Box(
                Modifier.layoutId(PrimaryPaneId)
                  .consumeWindowInsets(safeDrawing.only(WindowInsetsSides.End))
              ) {
                // Circuit marks only the top record active and pauses the rest. The primary record
                // stays visible here, so it follows the lifecycle of this decoration.
                ProvideRecordLifecycle(isActive = LocalRecordLifecycle.current.isActive) {
                  // Shared elements in the primary record need a Navigation scope while it sits
                  // outside the delegate's transitions.
                  ProvideStaticAnimatedTransitionScope(Navigation, Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalPaneRole provides PaneRole.PRIMARY) {
                      content(panes.primary)
                    }
                  }
                }
              }
              Box(Modifier.layoutId(DividerId)) { divider() }
            }
            val detailInsets =
              if (isSplit) {
                Modifier.consumeWindowInsets(safeDrawing.only(WindowInsetsSides.Start))
              } else {
                Modifier
              }
            Box(Modifier.layoutId(DetailPaneId).then(detailInsets)) {
              delegate.DecoratedContent(paneArgs, navigator, Modifier.fillMaxSize()) { arg ->
                when (arg) {
                  is PaneArg.Record -> {
                    // The left pane shows the primary record. The delegate can still ask for it
                    // while it animates out after a layout switch.
                    if (panes == null || arg.record.key != panes.primary.key) {
                      // Each entry keeps the role it had in its own stack, so a detail animating
                      // out keeps its chrome while a replacement animates in.
                      CompositionLocalProvider(LocalPaneRole provides arg.role) {
                        content(arg.record)
                      }
                    }
                  }
                  PaneArg.Placeholder -> placeholder?.invoke()
                }
              }
            }
          },
        ) { measurables, constraints ->
          val width = constraints.maxWidth
          val height = constraints.maxHeight
          val detail = measurables.first { it.layoutId == DetailPaneId }
          val primary = measurables.firstOrNull { it.layoutId == PrimaryPaneId }
          val dividerMeasurable = measurables.firstOrNull { it.layoutId == DividerId }
          if (panes == null || primary == null || dividerMeasurable == null) {
            val only = detail.measure(Constraints.fixed(width, height))
            return@Layout layout(width, height) { only.placeRelative(0, 0) }
          }
          // Every child is measured and placed in every branch, at zero width when hidden. Shared
          // element transitions run a lookahead pass, and a child placed without a lookahead
          // measurement throws.
          val dividerWidth =
            if (showsDetail) {
              dividerMeasurable.maxIntrinsicWidth(height).coerceIn(0, width)
            } else {
              0
            }
          val sceneWidth = panes.scene.primaryWidth
          val primaryWidth: Int
          val detailStart: Int
          when {
            !showsDetail -> {
              primaryWidth = width
              detailStart = width
            }
            sceneWidth != null -> {
              primaryWidth = (startInset + sceneWidth).roundToPx().coerceIn(0, width)
              detailStart = primaryWidth + dividerWidth
            }
            hinge != null -> {
              primaryWidth = hinge.left.roundToPx().coerceIn(0, width)
              detailStart = maxOf(hinge.right.roundToPx(), primaryWidth + dividerWidth)
            }
            else -> {
              primaryWidth = (width - dividerWidth) / 2
              detailStart = primaryWidth + dividerWidth
            }
          }
          val primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
          val dividerPlaceable = dividerMeasurable.measure(Constraints.fixed(dividerWidth, height))
          val detailPlaceable =
            detail.measure(Constraints.fixed((width - detailStart).coerceAtLeast(0), height))
          layout(width, height) {
            primaryPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(primaryWidth, 0)
            detailPlaceable.placeRelative(detailStart, 0)
          }
        }
      }
    }
  }
}

private const val PrimaryPaneId = "primary"
private const val DividerId = "divider"
private const val DetailPaneId = "detail"

/**
 * A resolved [scene]: the [primary] record for the left pane and the records [abovePrimary] (top
 * first) for the right pane.
 */
private data class Panes<T : NavArgument>(
  val scene: PaneScene,
  val primary: T,
  val abovePrimary: List<T>,
)

/** An entry in the delegate's stack: a back stack record, or the placeholder below the details. */
internal sealed interface PaneArg<out T : NavArgument> : NavArgument {
  /** A back stack [record], with the [role] it has in the stack it belongs to. */
  data class Record<T : NavArgument>(val record: T, val role: PaneRole) : PaneArg<T> {
    override val key: String
      get() = record.key

    override val screen: Screen
      get() = record.screen
  }

  data object Placeholder : PaneArg<Nothing> {
    override val key: String = "circuitx-adaptive-pane-placeholder"
    override val screen: Screen = PanePlaceholderScreen
  }
}

/** The placeholder's screen. It never enters the back stack. */
internal data object PanePlaceholderScreen : Screen

/** The records in [abovePrimary] (top first), over a placeholder. */
private fun <T : NavArgument> rightPaneStack(
  args: NavStackList<T>,
  abovePrimary: List<T>,
): NavStackList<PaneArg<T>> {
  val detailRootKey = abovePrimary.lastOrNull()?.key
  val placeholder: PaneArg<T> = PaneArg.Placeholder
  val toArg: (T) -> PaneArg<T> = { record ->
    val role =
      if (record.key == detailRootKey) {
        PaneRole.DETAIL_ROOT
      } else {
        PaneRole.DETAIL_STACKED
      }
    PaneArg.Record(record, role)
  }
  val forward = args.forwardItems.map(toArg)
  if (abovePrimary.isEmpty()) {
    return navStackListOf(forwardItems = forward, activeItem = placeholder)
  }
  return navStackListOf(
    forwardItems = forward,
    activeItem = toArg(abovePrimary.first()),
    backwardItems = abovePrimary.drop(1).map(toArg) + placeholder,
  )
}

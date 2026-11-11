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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isSpecified
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
 * The records of each pane are chosen in composition. The pane widths resolve in measure from the
 * [PaneSplit] of the scene. Shared element transitions add a lookahead pass, and both passes
 * resolve the same widths because they measure with the same constraints.
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
      val placeholder = host.placeholder ?: panes?.scene?.placeholder
      val showsDetail = panes != null && (panes.abovePrimary.isNotEmpty() || placeholder != null)
      val split = panes?.scene?.split
      // Alone in the window, the left pane always gets the full width.
      val primarySizesItself = showsDetail && split is PaneSplit.PrimaryWraps
      // Screens read this while they compose, so it carries the layout of this composition.
      val paneState =
        remember(panes, primarySizesItself) {
          PaneState(
            scene = panes?.scene,
            detailRoot = panes?.abovePrimary?.lastOrNull()?.screen,
            primarySizesItself = primarySizesItself,
          )
        }
      SideEffect {
        host.scene = paneState.scene
        host.detailRoot = paneState.detailRoot
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

      // The hinge for PaneSplit.AtHinge. A window without one splits in the middle.
      val hinge = paneWindow.separatingHinges.firstOrNull()
      // A pane that sizes itself keeps the width of its content, so nothing here may fill it.
      val primaryFill =
        if (primarySizesItself) {
          Modifier.fillMaxHeight()
        } else {
          Modifier.fillMaxSize()
        }

      CompositionLocalProvider(LocalPaneHost provides host, LocalPaneState provides paneState) {
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
                  ProvideStaticAnimatedTransitionScope(Navigation, primaryFill) {
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
          val scenePaneSplit = panes.scene.split
          val primaryPlaceable: Placeable
          val detailStart: Int
          when {
            !showsDetail -> {
              primaryPlaceable = primary.measure(Constraints.fixed(width, height))
              detailStart = width
            }
            scenePaneSplit is PaneSplit.Fixed -> {
              val primaryWidth =
                (startInset + scenePaneSplit.primaryWidth).roundToPx().coerceIn(0, width)
              primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
              detailStart = primaryWidth + dividerWidth
            }
            scenePaneSplit is PaneSplit.PrimaryWraps -> {
              // The left pane picks its width here. The same constraints go in on the lookahead
              // pass and on the main pass, so both get the same split.
              val room = (width - dividerWidth).coerceAtLeast(0)
              val maxWidth =
                if (scenePaneSplit.maxWidth.isSpecified) {
                  scenePaneSplit.maxWidth.roundToPx().coerceIn(0, room)
                } else {
                  room
                }
              primaryPlaceable =
                primary.measure(
                  Constraints(
                    minWidth = 0,
                    maxWidth = maxWidth,
                    minHeight = height,
                    maxHeight = height,
                  )
                )
              detailStart = primaryPlaceable.width + dividerWidth
            }
            hinge != null -> {
              val primaryWidth = hinge.left.roundToPx().coerceIn(0, width)
              primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
              detailStart = maxOf(hinge.right.roundToPx(), primaryWidth + dividerWidth)
            }
            else -> {
              val primaryWidth = (width - dividerWidth) / 2
              primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
              detailStart = primaryWidth + dividerWidth
            }
          }
          val primaryWidth = primaryPlaceable.width
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

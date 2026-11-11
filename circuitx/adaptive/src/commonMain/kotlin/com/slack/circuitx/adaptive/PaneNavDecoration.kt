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
 * Shows records side by side when [strategy] returns a [PaneScene], and passes the whole stack to
 * [delegate] otherwise.
 *
 * Circuit keeps one back stack in every layout. A fold or unfold only changes how the stack
 * renders, so records never move between stacks and keep their retained and saved state.
 *
 * In a split layout:
 * - The left pane shows the primary record of the scene.
 * - A middle pane shows the secondary record, when the scene has one.
 * - The right pane passes the records above those to [delegate], with a placeholder below them.
 *   Back gestures and their animations act on the right pane only, and going back from the first
 *   detail reveals the placeholder, like the secondary column of an iPad split view.
 *
 * The right pane always renders through the same [delegate] call, so a layout switch never remounts
 * the record that stays there. Circuit wraps each record's content in movable content, so a record
 * keeps its state when it moves between a pane and [delegate]. Each record renders in one place
 * only, because two calls would reuse the same saved state keys.
 *
 * The records of each pane are chosen in composition. The pane widths resolve in measure from the
 * scene. Shared element transitions add a lookahead pass, and both passes resolve the same widths
 * because they measure with the same constraints.
 *
 * @param strategy Decides when to split and which records take the left and middle panes.
 * @param delegate Renders the single layout, and the right pane of a split layout.
 * @param host Receives the layout this decoration chose. Share it with [PaneReplaceInterceptor].
 * @param window Creates the [PaneWindow] for the size of this decoration. Override it to supply
 *   hinges from another source, or to change how many panes fit and how wide they are.
 * @param divider Drawn between panes, at its intrinsic width.
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
          strategy.scene(paneWindow, args)?.let { scene -> resolvePanes(scene, visible) }
        }
      val isSplit = panes != null
      val placeholder = host.placeholder ?: panes?.scene?.placeholder
      val showsDetail = panes != null && (panes.abovePrimary.isNotEmpty() || placeholder != null)
      // Alone in the window, the left pane always gets the full width.
      val primarySizesItself = showsDetail && panes.scene.split is PaneSplit.PrimaryWraps
      // Screens read this while they compose, so it carries the layout of this composition.
      val paneState =
        remember(panes, primarySizesItself, paneWindow) {
          PaneState(
            scene = panes?.scene,
            detailRoot = panes?.abovePrimary?.lastOrNull()?.screen,
            primarySizesItself = primarySizesItself,
            preferredPaneWidth = paneWindow.preferredPaneWidth,
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
            rightPaneStack(args, panes)
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

      CompositionLocalProvider(LocalPaneHost provides host, LocalPaneState provides paneState) {
        Layout(
          modifier = Modifier.fillMaxSize(),
          content = {
            if (panes != null) {
              Box(
                Modifier.layoutId(PrimaryPaneId)
                  .consumeWindowInsets(safeDrawing.only(WindowInsetsSides.End))
              ) {
                // A pane that sizes itself keeps the width of its content, so nothing here may
                // fill it.
                StaticPane(PaneRole.PRIMARY, fillWidth = !primarySizesItself) {
                  content(panes.primary)
                }
              }
              Box(Modifier.layoutId(DividerId)) { divider() }
            }
            if (panes?.secondary != null) {
              Box(
                Modifier.layoutId(SecondaryPaneId)
                  .consumeWindowInsets(safeDrawing.only(WindowInsetsSides.Horizontal))
              ) {
                StaticPane(panes.roleOf(panes.secondary), fillWidth = true) {
                  content(panes.secondary)
                }
              }
              Box(Modifier.layoutId(SecondDividerId)) { divider() }
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
                    // The left and middle panes show their records. The delegate can still ask
                    // for one of them while it animates out after a layout switch.
                    if (panes == null || !panes.isStatic(arg.record)) {
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
          val secondary = measurables.firstOrNull { it.layoutId == SecondaryPaneId }
          val secondDividerMeasurable = measurables.firstOrNull { it.layoutId == SecondDividerId }
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
          // Where the area beside the left pane starts.
          val besideStart: Int
          when {
            !showsDetail -> {
              primaryPlaceable = primary.measure(Constraints.fixed(width, height))
              besideStart = width
            }
            scenePaneSplit is PaneSplit.Fixed -> {
              val primaryWidth =
                (startInset + scenePaneSplit.primaryWidth).roundToPx().coerceIn(0, width)
              primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
              besideStart = primaryWidth + dividerWidth
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
              besideStart = primaryPlaceable.width + dividerWidth
            }
            hinge != null -> {
              val primaryWidth = hinge.left.roundToPx().coerceIn(0, width)
              primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
              besideStart = maxOf(hinge.right.roundToPx(), primaryWidth + dividerWidth)
            }
            else -> {
              val primaryWidth = (width - dividerWidth) / 2
              primaryPlaceable = primary.measure(Constraints.fixed(primaryWidth, height))
              besideStart = primaryWidth + dividerWidth
            }
          }
          val primaryWidth = primaryPlaceable.width
          val dividerPlaceable = dividerMeasurable.measure(Constraints.fixed(dividerWidth, height))
          val besideWidth = (width - besideStart).coerceAtLeast(0)
          if (secondary == null || secondDividerMeasurable == null) {
            val detailPlaceable = detail.measure(Constraints.fixed(besideWidth, height))
            return@Layout layout(width, height) {
              primaryPlaceable.placeRelative(0, 0)
              dividerPlaceable.placeRelative(primaryWidth, 0)
              detailPlaceable.placeRelative(besideStart, 0)
            }
          }
          // Three panes. The right pane has a set width and the middle pane takes what is left.
          val secondDividerWidth =
            secondDividerMeasurable.maxIntrinsicWidth(height).coerceIn(0, besideWidth)
          val rightRoom = besideWidth - secondDividerWidth
          val rightWidth =
            if (panes.scene.rightPaneWidth.isSpecified) {
              panes.scene.rightPaneWidth.roundToPx().coerceIn(0, rightRoom)
            } else {
              rightRoom / 2
            }
          val secondaryWidth = rightRoom - rightWidth
          val secondaryPlaceable = secondary.measure(Constraints.fixed(secondaryWidth, height))
          val secondDividerPlaceable =
            secondDividerMeasurable.measure(Constraints.fixed(secondDividerWidth, height))
          val detailPlaceable = detail.measure(Constraints.fixed(rightWidth, height))
          layout(width, height) {
            primaryPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(primaryWidth, 0)
            secondaryPlaceable.placeRelative(besideStart, 0)
            secondDividerPlaceable.placeRelative(besideStart + secondaryWidth, 0)
            detailPlaceable.placeRelative(besideStart + secondaryWidth + secondDividerWidth, 0)
          }
        }
      }
    }
  }
}

/**
 * Renders a record that stays in a pane outside the delegate, so it has no transitions of its own.
 *
 * @param fillWidth False for a pane that sizes itself, which must keep the width of its content.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StaticPane(role: PaneRole, fillWidth: Boolean, content: @Composable () -> Unit) {
  val fill =
    if (fillWidth) {
      Modifier.fillMaxSize()
    } else {
      Modifier.fillMaxHeight()
    }
  // Circuit marks only the top record active and pauses the rest. This record stays visible, so
  // it follows the lifecycle of the decoration.
  ProvideRecordLifecycle(isActive = LocalRecordLifecycle.current.isActive) {
    // Shared elements in the record need a Navigation scope while it sits outside the delegate's
    // transitions.
    ProvideStaticAnimatedTransitionScope(Navigation, fill) {
      CompositionLocalProvider(LocalPaneRole provides role, content = content)
    }
  }
}

private const val PrimaryPaneId = "primary"
private const val DividerId = "divider"
private const val SecondaryPaneId = "secondary"
private const val SecondDividerId = "second-divider"
private const val DetailPaneId = "detail"

/**
 * A resolved [scene].
 *
 * @property primary The record for the left pane.
 * @property abovePrimary Every record above [primary], top first.
 * @property secondary The record for the middle pane, or null for two panes.
 * @property rightPane The records for the right pane, top first: those above [secondary], or
 *   [abovePrimary] for two panes.
 */
private data class Panes<T : NavArgument>(
  val scene: PaneScene,
  val primary: T,
  val abovePrimary: List<T>,
  val secondary: T?,
  val rightPane: List<T>,
) {
  /** Whether [record] renders in a pane outside the delegate. */
  fun isStatic(record: T): Boolean {
    return record.key == primary.key || record.key == secondary?.key
  }

  /**
   * The role of a record above [primary]. A record keeps its detail role when it moves to the
   * middle pane, so its chrome stays the same.
   */
  fun roleOf(record: T): PaneRole {
    val inExtraPane = secondary != null && rightPane.any { it.key == record.key }
    return when {
      inExtraPane && record.key == rightPane.last().key -> PaneRole.EXTRA_ROOT
      inExtraPane -> PaneRole.EXTRA_STACKED
      record.key == abovePrimary.lastOrNull()?.key -> PaneRole.DETAIL_ROOT
      else -> PaneRole.DETAIL_STACKED
    }
  }
}

/** Finds the records that [scene] names in [visible] (top first), or null when it names none. */
private fun <T : NavArgument> resolvePanes(scene: PaneScene, visible: List<T>): Panes<T>? {
  val primaryIndex = visible.indexOfFirst { it.key == scene.primaryKey }
  if (primaryIndex < 0) {
    return null
  }
  val abovePrimary = visible.subList(0, primaryIndex)
  // A middle pane needs a record of its own and at least one record above it for the right pane.
  val secondaryIndex =
    scene.secondaryKey
      ?.let { key -> abovePrimary.indexOfFirst { it.key == key } }
      ?.takeIf { it > 0 }
  return Panes(
    scene = scene,
    primary = visible[primaryIndex],
    abovePrimary = abovePrimary,
    secondary = secondaryIndex?.let { abovePrimary[it] },
    rightPane = secondaryIndex?.let { abovePrimary.subList(0, it) } ?: abovePrimary,
  )
}

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

/** The records of the right pane of [panes] (top first), over a placeholder. */
private fun <T : NavArgument> rightPaneStack(
  args: NavStackList<T>,
  panes: Panes<T>,
): NavStackList<PaneArg<T>> {
  val rightPane = panes.rightPane
  val placeholder: PaneArg<T> = PaneArg.Placeholder
  val toArg: (T) -> PaneArg<T> = { record -> PaneArg.Record(record, panes.roleOf(record)) }
  // Forward records are above the top of the stack, so they stack over the right pane's records.
  val forwardRole =
    if (panes.secondary != null) {
      PaneRole.EXTRA_STACKED
    } else {
      PaneRole.DETAIL_STACKED
    }
  val forward = args.forwardItems.map { record -> PaneArg.Record(record, forwardRole) }
  if (rightPane.isEmpty()) {
    return navStackListOf(forwardItems = forward, activeItem = placeholder)
  }
  return navStackListOf(
    forwardItems = forward,
    activeItem = toArg(rightPane.first()),
    backwardItems = rightPane.drop(1).map(toArg) + placeholder,
  )
}

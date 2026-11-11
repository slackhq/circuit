// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
 * @param animationSpec Moves the pane edges when a pane opens, closes, or changes width. The left
 *   and middle panes measure again on each frame of it. Pass `snap()` to resize them in one frame,
 *   which costs less for a heavy left pane.
 */
@ExperimentalCircuitAdaptiveApi
@Stable
public class PaneNavDecoration(
  private val strategy: PaneStrategy,
  private val delegate: NavDecoration,
  private val host: PaneHost = PaneHost(),
  private val window: @Composable (DpSize) -> PaneWindow = { rememberPaneWindow(it) },
  private val divider: @Composable () -> Unit = {},
  private val animationSpec: FiniteAnimationSpec<Float> = DefaultPaneAnimationSpec,
) : NavDecoration {

  @Composable
  override fun <T : NavArgument> DecoratedContent(
    args: NavStackList<T>,
    navigator: Navigator,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
  ) {
    BoxWithConstraints(modifier) {
      // ---- 1. Describe the window ----

      // Hinge bounds arrive in window coordinates. This decoration can start past the edge of
      // the window, like beside a navigation rail, so the strategy and the layout get the
      // hinges in the decoration's own coordinates, measured from its start edge. The position
      // is known after the first placement, so the first frame uses zero.
      var leftInWindow by remember { mutableIntStateOf(0) }
      val windowFromApp = window(DpSize(maxWidth, maxHeight))
      val leftInWindowDp = with(LocalDensity.current) { leftInWindow.toDp() }
      val layoutDirection = LocalLayoutDirection.current
      val paneWindow =
        remember(windowFromApp, leftInWindowDp, layoutDirection) {
          windowFromApp.withHingesInDecoration(leftInWindowDp, layoutDirection)
        }

      // ---- 2. Ask the strategy which records take which pane ----

      // Top first, root last. Forward items stay hidden, as Circuit shows the active record.
      val visible = remember(args) { listOf(args.active) + args.backwardItems }
      // Null means the single layout: the delegate gets the whole stack.
      val panes =
        remember(args, visible, paneWindow) {
          strategy.scene(paneWindow, args)?.let { scene -> resolvePanes(scene, visible) }
        }
      val isSplit = panes != null
      val hostPlaceholder = host.placeholder
      val placeholder =
        when {
          hostPlaceholder == null -> panes?.scene?.placeholder
          hostPlaceholder === PaneHost.NoPlaceholder -> null
          else -> hostPlaceholder
        }
      // A split scene still shows one pane while nothing is open beside the primary record and
      // nothing reserves that space.
      val showsDetail = panes != null && (panes.abovePrimary.isNotEmpty() || placeholder != null)
      // Alone in the window, the left pane always gets the full width.
      val primarySizesItself = showsDetail && panes.scene.split is PaneSplit.PrimaryWraps
      val paneCount =
        when {
          !showsDetail -> 1
          panes.secondary != null -> 3
          else -> 2
        }

      // ---- 3. Publish the layout ----

      // Screens read this while they compose, so it carries the layout of this composition.
      val paneState =
        remember(panes, primarySizesItself, paneWindow, paneCount) {
          PaneState(
            scene = panes?.scene,
            detailRoot = panes?.abovePrimary?.lastOrNull()?.screen,
            primarySizesItself = primarySizesItself,
            preferredPaneWidth = paneWindow.preferredPaneWidth,
            paneCount = paneCount,
          )
        }
      // The host serves readers outside composition, like PaneReplaceInterceptor, so it gets the
      // layout once this composition is applied.
      SideEffect {
        host.scene = paneState.scene
        host.detailRoot = paneState.detailRoot
        host.paneCount = paneState.paneCount
        if (panes != null) {
          host.primaryDepth = visible.size - panes.abovePrimary.size
        }
      }

      // ---- 4. Build the stack that the delegate renders ----

      // Split: only the right pane's records, over a placeholder. Single: every record.
      val paneArgs =
        remember(args, panes) {
          if (panes != null) {
            rightPaneStack(args, panes)
          } else {
            args.transform<T, PaneArg<T>> { PaneArg.Record(it, PaneRole.SINGLE) }
          }
        }

      // ---- 5. Inputs for the layout below ----

      val safeDrawing = WindowInsets.safeDrawing
      val startInset =
        safeDrawing
          .only(WindowInsetsSides.Start)
          .asPaddingValues()
          .calculateStartPadding(layoutDirection)
      val endInset =
        safeDrawing
          .only(WindowInsetsSides.End)
          .asPaddingValues()
          .calculateEndPadding(layoutDirection)

      // The hinge for PaneSplit.AtHinge. A window without one splits in the middle.
      val hinge = paneWindow.separatingHinges.firstOrNull()
      // The hinge of the last measure. It moves one frame after the first placement, when the
      // decoration learns where it is in the window. That move is a correction, so the edges jump
      // to it.
      val lastHinge = remember { arrayOfNulls<DpRect>(1) }
      // The two edges that move when a pane opens or closes. See the diagram at the measure
      // block below.
      val besideEdge = remember { PaneEdge() }
      val rightEdge = remember { PaneEdge() }
      // The edge that the right pane starts at, or null for the single layout.
      var detailEdge by remember { mutableStateOf<PaneEdge?>(null) }
      val animationScope = rememberCoroutineScope()

      CompositionLocalProvider(LocalPaneHost provides host, LocalPaneState provides paneState) {
        Layout(
          modifier =
            Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
              leftInWindow = coordinates.positionInWindow().x.roundToInt()
            },
          content = {
            // Up to five children, each found by its layoutId in the measure block:
            // the left pane, a divider, the middle pane, a second divider, and the delegate.

            // Left pane and its divider. The pane does not reach the end edge of the window, so
            // it takes no end inset.
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
            // Middle pane and its divider, for three panes. It touches neither window edge.
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
            // The delegate. It is the whole layout when single, and the right pane when split.
            // It is always this one call, so a layout switch never remounts what it shows.
            val detailInsets =
              if (isSplit) {
                Modifier.consumeWindowInsets(safeDrawing.only(WindowInsetsSides.Start))
              } else {
                Modifier
              }
            Box(
              Modifier.layoutId(DetailPaneId)
                // The right pane stays at its final layout position and slides with a draw
                // offset. Shared elements inside it then see one stable position, where a
                // moving layout position makes them lose their place.
                .graphicsLayer {
                  val offset = detailEdge?.offsetFromTarget ?: 0f
                  // Edges are measured from the start edge, which is on the right in RTL.
                  translationX =
                    if (layoutDirection == LayoutDirection.Rtl) {
                      -offset
                    } else {
                      offset
                    }
                }
                .then(detailInsets)
            ) {
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
          // The names used below, for two panes and for three:
          //
          //   0        primaryWidth  besideStart                      width
          //   |  left pane  | divider |          right pane             |
          //
          //   0        primaryWidth  besideStart            rightStart  width
          //   |  left pane  | divider | middle pane | divider | right pane |
          //
          // A "target" value is where an edge ends up. The value without that prefix is where
          // the edge is in this frame, while it animates to the target.
          val width = constraints.maxWidth
          val height = constraints.maxHeight
          val detail = measurables.first { it.layoutId == DetailPaneId }
          val primary = measurables.firstOrNull { it.layoutId == PrimaryPaneId }
          val dividerMeasurable = measurables.firstOrNull { it.layoutId == DividerId }

          // ---- Single layout: the delegate fills the decoration ----
          if (panes == null || primary == null || dividerMeasurable == null) {
            // The top record has the full width. If the next layout splits at this width, the
            // left pane resizes from here and the right pane slides in. A third pane starts at its
            // final width.
            besideEdge.snapTo(width, width)
            rightEdge.reset()
            detailEdge = null
            lastHinge[0] = hinge
            val only = detail.measure(Constraints.fixed(width, height))
            return@Layout layout(width, height) { only.placeRelative(0, 0) }
          }
          val secondary = measurables.firstOrNull { it.layoutId == SecondaryPaneId }
          val secondDividerMeasurable = measurables.firstOrNull { it.layoutId == SecondDividerId }
          // Every child is measured and placed in every branch, off screen when hidden. Shared
          // element transitions run a lookahead pass, and a child placed without a lookahead
          // measurement throws.
          val dividerWidth =
            if (showsDetail) {
              dividerMeasurable.maxIntrinsicWidth(height).coerceIn(0, width)
            } else {
              0
            }

          val hingeMoved = lastHinge[0] != hinge
          lastHinge[0] = hinge

          // ---- Where the left pane ends, from the scene's split ----
          val scenePaneSplit = panes.scene.split
          // The left pane when it picks its own width. It is measured once, at its final size.
          var wrappedPrimary: Placeable? = null
          // The final width of the left pane, and where the area beside it starts.
          val targetPrimaryWidth: Int
          val targetBesideStart: Int
          when {
            // Nothing beside the left pane, so it takes the full width.
            !showsDetail -> {
              targetPrimaryWidth = width
              targetBesideStart = width
            }
            // A width from the strategy, plus the start inset that the left pane covers.
            scenePaneSplit is PaneSplit.Fixed -> {
              targetPrimaryWidth =
                (startInset + scenePaneSplit.primaryWidth).roundToPx().coerceIn(0, width)
              targetBesideStart = targetPrimaryWidth + dividerWidth
            }
            // A width for the right pane, plus the end inset that it covers. The divider takes
            // its width from the left pane.
            scenePaneSplit is PaneSplit.RightFixed -> {
              val rightWidth = (endInset + scenePaneSplit.rightWidth).roundToPx().coerceIn(0, width)
              targetBesideStart = width - rightWidth
              targetPrimaryWidth = (targetBesideStart - dividerWidth).coerceAtLeast(0)
            }
            scenePaneSplit is PaneSplit.Fraction -> {
              targetPrimaryWidth =
                ((width - dividerWidth) * scenePaneSplit.primaryFraction).roundToInt()
              targetBesideStart = targetPrimaryWidth + dividerWidth
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
              val placeable =
                primary.measure(
                  Constraints(
                    minWidth = 0,
                    maxWidth = maxWidth,
                    minHeight = height,
                    maxHeight = height,
                  )
                )
              wrappedPrimary = placeable
              targetPrimaryWidth = placeable.width
              targetBesideStart = targetPrimaryWidth + dividerWidth
            }
            // PaneSplit.AtHinge with a hinge: the left pane ends where the hinge starts, and the
            // next pane starts where the hinge ends.
            hinge != null -> {
              targetPrimaryWidth = hinge.left.roundToPx().coerceIn(0, width)
              targetBesideStart = maxOf(hinge.right.roundToPx(), targetPrimaryWidth + dividerWidth)
            }
            // PaneSplit.AtHinge with no hinge: two equal panes.
            else -> {
              targetPrimaryWidth = (width - dividerWidth) / 2
              targetBesideStart = targetPrimaryWidth + dividerWidth
            }
          }

          // ---- Animate that edge and measure the left pane ----
          // The edge moves to its target over a few frames, so the left pane resizes while the
          // right pane slides. The lookahead pass gets the same position as the main pass.
          val besideStart =
            besideEdge.animateTo(
              newTarget = targetBesideStart,
              width = width,
              coroutineScope = animationScope,
              animationSpec = animationSpec,
              snap = hingeMoved,
            )
          // The gap between the left pane and the next one (a divider or a hinge) keeps its
          // size while the edge moves.
          val gapWidth = targetBesideStart - targetPrimaryWidth
          val primaryWidth = (besideStart - gapWidth).coerceAtLeast(0)
          val primaryPlaceable =
            wrappedPrimary ?: primary.measure(Constraints.fixed(primaryWidth, height))
          val dividerPlaceable = dividerMeasurable.measure(Constraints.fixed(dividerWidth, height))
          val targetBesideWidth = (width - targetBesideStart).coerceAtLeast(0)

          // ---- Two panes: the delegate takes everything beside the left pane ----
          if (secondary == null || secondDividerMeasurable == null) {
            // The right pane keeps its full width while it slides, so its content does not reflow
            // on the way. After it closes, it keeps the last width it had, off screen.
            if (targetBesideWidth > 0) {
              besideEdge.lastPaneWidth = targetBesideWidth
            }
            val detailPlaceable =
              detail.measure(Constraints.fixed(besideEdge.lastPaneWidth, height))
            // A third pane opens from where the right pane is now.
            rightEdge.snapTo(besideStart, width)
            detailEdge = besideEdge
            return@Layout layout(width, height) {
              primaryPlaceable.placeRelative(0, 0)
              dividerPlaceable.placeRelative(primaryWidth, 0)
              // At its target. The draw offset from detailEdge makes it slide.
              detailPlaceable.placeRelative(targetBesideStart, 0)
            }
          }

          // ---- Three panes: the delegate has a set width, the middle pane takes the rest ----
          val secondDividerWidth =
            secondDividerMeasurable.maxIntrinsicWidth(height).coerceIn(0, targetBesideWidth)
          val rightRoom = targetBesideWidth - secondDividerWidth
          val rightWidth =
            if (panes.scene.rightPaneWidth.isSpecified) {
              panes.scene.rightPaneWidth.roundToPx().coerceIn(0, rightRoom)
            } else {
              rightRoom / 2
            }
          val targetRightStart = width - rightWidth
          val rightStart =
            rightEdge.animateTo(
              newTarget = targetRightStart,
              width = width,
              coroutineScope = animationScope,
              animationSpec = animationSpec,
              snap = hingeMoved,
            )
          detailEdge = rightEdge
          // The middle pane sits between the two moving edges, so it resizes as either one moves.
          val secondaryWidth = (rightStart - secondDividerWidth - besideStart).coerceAtLeast(0)
          val secondaryPlaceable = secondary.measure(Constraints.fixed(secondaryWidth, height))
          val secondDividerPlaceable =
            secondDividerMeasurable.measure(Constraints.fixed(secondDividerWidth, height))
          val detailPlaceable = detail.measure(Constraints.fixed(rightWidth, height))
          besideEdge.lastPaneWidth = rightWidth
          layout(width, height) {
            primaryPlaceable.placeRelative(0, 0)
            dividerPlaceable.placeRelative(primaryWidth, 0)
            secondaryPlaceable.placeRelative(besideStart, 0)
            secondDividerPlaceable.placeRelative(besideStart + secondaryWidth, 0)
            detailPlaceable.placeRelative(targetRightStart, 0)
          }
        }
      }
    }
  }
}

/**
 * The position of a pane edge, in pixels from the start of the decoration. It moves to each new
 * target with an animation, and jumps when the decoration first lays out or changes width, like on
 * a rotation.
 */
@Stable
private class PaneEdge {
  private var position by mutableFloatStateOf(0f)
  private var animation: Job? = null
  private var initialized = false
  private var target = 0
  private var layoutWidth = -1

  /** The last width of the pane that starts at this edge, kept for after the pane closes. */
  var lastPaneWidth = 0

  /**
   * How far the edge is from its target, in pixels. The right pane draws with this offset, so it
   * slides while its layout position stays at the target.
   */
  val offsetFromTarget: Float
    get() = position - target

  /** Moves the edge to [newPosition] with no animation. */
  fun snapTo(newPosition: Int, width: Int) {
    if (!initialized || target != newPosition || layoutWidth != width) {
      animation?.cancel()
      position = newPosition.toFloat()
      target = newPosition
      layoutWidth = width
      initialized = true
    }
  }

  /**
   * Returns where the edge is in this frame on its way to [newTarget]. The read of the position
   * makes the layout measure again on each frame of the animation.
   *
   * @param snap Jumps to [newTarget], for a move that corrects the layout.
   */
  fun animateTo(
    newTarget: Int,
    width: Int,
    coroutineScope: CoroutineScope,
    animationSpec: FiniteAnimationSpec<Float>,
    snap: Boolean,
  ): Int {
    // First layout, or the decoration changed width: nothing sensible to animate from.
    if (!initialized || layoutWidth != width || snap || animationSpec is SnapSpec) {
      snapTo(newTarget, width)
      return newTarget
    }
    // A new target at the same width, like a pane that opened: start from where the edge is
    // now, which also covers a target that changes mid-animation.
    if (target != newTarget) {
      val from = position
      target = newTarget
      animation?.cancel()
      animation = coroutineScope.launch {
        animate(from, newTarget.toFloat(), animationSpec = animationSpec) { value, _ ->
          position = value
        }
      }
    }
    return position.roundToInt()
  }

  /** Forgets the position, so the next layout jumps to its target. */
  fun reset() {
    animation?.cancel()
    initialized = false
    layoutWidth = -1
  }
}

/** The default pane edge animation. Its threshold is one pixel, the unit of an edge position. */
private val DefaultPaneAnimationSpec: FiniteAnimationSpec<Float> =
  spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)

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

/**
 * Returns this window with its hinges moved from window coordinates to the coordinates of a
 * decoration whose left edge is [left] from the left edge of the window. Hinges outside the
 * decoration are dropped.
 *
 * The result measures each hinge from the start edge of the decoration, which is its right edge in
 * a right-to-left layout. The layout places panes from the start edge, so a pane that ends at
 * `hinge.left` ends at the hinge in both directions.
 */
private fun PaneWindow.withHingesInDecoration(
  left: Dp,
  layoutDirection: LayoutDirection,
): PaneWindow {
  if (separatingHinges.isEmpty()) {
    return this
  }
  val shifted =
    separatingHinges
      .map { hinge ->
        val hingeLeft = hinge.left - left
        val hingeRight = hinge.right - left
        if (layoutDirection == LayoutDirection.Rtl) {
          DpRect(size.width - hingeRight, hinge.top, size.width - hingeLeft, hinge.bottom)
        } else {
          DpRect(hingeLeft, hinge.top, hingeRight, hinge.bottom)
        }
      }
      .filter { it.left > 0.dp && it.right < size.width }
  if (shifted == separatingHinges) {
    return this
  }
  return PaneWindow(
    size = size,
    sizeClass = sizeClass,
    separatingHinges = shifted,
    maxPanes = maxPanes,
    preferredPaneWidth = preferredPaneWidth,
  )
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
   *
   * For the stack `Profile, Thread, Conversation, Inbox` (top first) with `Inbox` as [primary]:
   * - Two panes: `Conversation` is `DETAIL_ROOT`, the two above it are `DETAIL_STACKED`.
   * - Three panes with `Thread` as [secondary]: `Profile` is `EXTRA_ROOT`, `Thread` is
   *   `DETAIL_STACKED`, and the hidden `Conversation` is still `DETAIL_ROOT`.
   */
  fun roleOf(record: T): PaneRole {
    // The right pane holds extra screens only while a middle pane exists.
    val inExtraPane = secondary != null && rightPane.any { it.key == record.key }
    return when {
      // Lists are top first, so the last entry is the bottom of its pane.
      inExtraPane && record.key == rightPane.last().key -> PaneRole.EXTRA_ROOT
      inExtraPane -> PaneRole.EXTRA_STACKED
      record.key == abovePrimary.lastOrNull()?.key -> PaneRole.DETAIL_ROOT
      else -> PaneRole.DETAIL_STACKED
    }
  }
}

/**
 * Finds the records that [scene] names in [visible] (top first), or null when it names none.
 *
 * For `visible = [Profile, Conversation, Inbox, Home]` and a scene with `Inbox` as primary:
 * - `abovePrimary` is `[Profile, Conversation]`, and `Home` stays hidden.
 * - With no secondary key, `rightPane` is `[Profile, Conversation]`.
 * - With `Conversation` as secondary, `rightPane` is `[Profile]`.
 */
private fun <T : NavArgument> resolvePanes(scene: PaneScene, visible: List<T>): Panes<T>? {
  val primaryIndex = visible.indexOfFirst { it.key == scene.primaryKey }
  if (primaryIndex < 0) {
    return null
  }
  // Lower indexes are higher on the stack, so everything before the primary is above it.
  val abovePrimary = visible.subList(0, primaryIndex)
  // A middle pane needs a record of its own and at least one record above it for the right pane.
  // These two splits describe two panes only.
  val splitFitsThreePanes =
    scene.split !is PaneSplit.AtHinge && scene.split !is PaneSplit.RightFixed
  val secondaryIndex =
    scene.secondaryKey
      ?.takeIf { splitFitsThreePanes }
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

/**
 * The records of the right pane of [panes] (top first), over a placeholder.
 *
 * The placeholder is the root of this stack. The delegate then treats the first record in the right
 * pane as a push over it, so that record gets an enter transition, and a pop back to the left
 * pane's record alone plays as a pop to the placeholder.
 *
 * For a right pane of `[Profile, Conversation]`, the delegate gets `Profile` as active, with
 * `Conversation` and then the placeholder behind it. For an empty right pane, the placeholder is
 * the only entry.
 */
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

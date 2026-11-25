// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.LocalRecordLifecycle
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.animation.AnimatedNavDecoration
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.ExperimentalCircuitApi
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.presenterOf
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.ui
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.navigation.intercepting.InterceptingNavigator
import kotlin.test.Test

/** Renders real Circuit navigation through [PaneNavDecoration] at phone and wide window sizes. */
@OptIn(
  ExperimentalTestApi::class,
  ExperimentalCircuitApi::class,
  ExperimentalSharedTransitionApi::class,
)
class PaneNavDecorationTest {
  private data object Root : Screen

  /** A root that reserves the end pane, like a list with a "pick one" message. */
  private data object ReservingRoot : Screen

  /** A root that draws a rail beside its list and picks its own width in the start pane. */
  private data object SelfSizedRoot : Screen

  private data class Detail(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.ListDetail
  }

  private data class Supporting(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.Supporting
  }

  /** A screen opened from a detail, so it stacks in the end pane. */
  private data class Stacked(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.ListDetail
  }

  /** A screen opened from a detail that takes a third pane when the window has room. */
  private data class Extra(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.Extra
  }

  private data class TestState(val screen: Screen) : CircuitUiState

  private val phone = DpSize(411.dp, 900.dp)
  /** An iPad mini in portrait, which keeps the phone layout. */
  private val portraitTablet = DpSize(744.dp, 1133.dp)
  // The test window is 1024px wide at density 1, so wider sizes would clamp
  private val tablet = DpSize(1000.dp, 700.dp)
  /** The list pane at [tablet]: Material's preferred pane width. */
  private val tabletListPaneWidth = 360.dp
  private val RailWidth = 72.dp

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, _, _ -> presenterOf { TestState(screen) } }
      .addUiFactory { _, _ -> ui<TestState> { state, modifier -> TestUi(state.screen, modifier) } }
      .build()

  private var windowSize by mutableStateOf(phone)
  private var hinge: DpRect? = null
  /** Width of app chrome before the decoration, like a navigation rail. */
  private var leadingWidth by mutableStateOf(0.dp)
  private var layoutDirection = LayoutDirection.Ltr
  /** Stands in for the safe drawing insets, because the test window has none. */
  private var windowInsets: WindowInsets? = null
  /** Insets that the app chrome before the decoration handles. */
  private var consumedByChrome by mutableStateOf(WindowInsets(0, 0, 0, 0))
  /** Overrides how many panes fit, because the test window is too narrow for three by default. */
  private var maxPanes: Int? = null
  private val host = PaneHost()
  private var strategy: PaneStrategy = ListDetailPaneStrategy()
  /** App chrome around each pane. The default draws the pane only. */
  private var paneContainer: @Composable (PanePosition, @Composable () -> Unit) -> Unit =
    { _, content ->
      content()
    }
  /** The drag handle between two panes, or null for none. */
  private var dragHandle: (@Composable () -> Unit)? = null
  /** The [PaneState] of each composition of the root screen, oldest first. */
  private val rootPaneStates = mutableListOf<PaneState>()
  private lateinit var navigator: Navigator

  private fun ComposeUiTest.setNavContent(root: Screen) {
    if (root == ReservingRoot) {
      host.placeholder = { BasicText("Pick one") }
    }
    setContent {
      CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        NavContent(root)
      }
    }
  }

  @Composable
  private fun NavContent(root: Screen) {
    run {
      CircuitCompositionLocals(circuit) {
        // Apps wrap navigation in this, which adds a lookahead pass to every layout
        SharedElementTransitionLayout {
          Box(
            Modifier.size(windowSize)
              .padding(start = leadingWidth)
              .consumeWindowInsets(consumedByChrome)
          ) {
            val backStack = rememberSaveableBackStack(root)
            val circuitNavigator = rememberCircuitNavigator(backStack) {}
            navigator =
              remember(circuitNavigator) {
                InterceptingNavigator(circuitNavigator, listOf(PaneReplaceInterceptor(host)))
              }
            val decoration = remember {
              PaneNavDecoration(
                  strategy = strategy,
                  delegate =
                    AnimatedNavDecoration(emptyMap(), NavigatorDefaults.DefaultDecoratorFactory),
                  host = host,
                  // Material adaptive reads the real test window, which never has a hinge
                  window = { size -> testWindow(size) },
                  paneContainer = paneContainer,
                  dragHandle = dragHandle,
                )
                .apply { windowInsets?.let { insets -> safeDrawingInsets = { insets } } }
            }
            NavigableCircuitContent(navigator, backStack, decoration = decoration)
          }
        }
      }
    }
  }

  private fun testWindow(size: DpSize): PaneWindow {
    val window = PaneWindow(size, listOfNotNull(hinge))
    val panes = maxPanes ?: return window
    return PaneWindow(
      size = window.size,
      sizeClass = window.sizeClass,
      separatingHinges = window.separatingHinges,
      maxPanes = panes,
      preferredPaneWidth = window.preferredPaneWidth,
    )
  }

  private fun ComposeUiTest.assertLabel(tag: String, role: PaneRole, taps: Int) {
    onNodeWithTag(tag).assertTextEquals("$tag role=$role active=true taps=$taps")
  }

  @Test
  fun phoneShowsOnlyTheTopScreen() = runComposeUiTest {
    windowSize = phone
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    assertLabel("detail1", PaneRole.Single, taps = 0)
    onAllNodesWithTag("root").assertCountEquals(0)
  }

  @Test
  fun wideWindowShowsRootBesideDetail() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    assertLabel("root", PaneRole.Primary, taps = 0)
    assertLabel("detail1", PaneRole.DetailRoot, taps = 0)

    runOnIdle { navigator.goTo(Stacked(1)) }
    assertLabel("stacked1", PaneRole.DetailStacked, taps = 0)
    onAllNodesWithTag("root").assertCountEquals(1)
  }

  @Test
  fun mediumWindowKeepsThePhoneLayoutUntilRotated() = runComposeUiTest {
    windowSize = portraitTablet
    setNavContent(ReservingRoot)
    assertLabel("root", PaneRole.Single, taps = 0)
    onAllNodesWithText("Pick one").assertCountEquals(0)

    runOnIdle { navigator.goTo(Supporting(1)) }
    assertLabel("supporting1", PaneRole.Single, taps = 0)
    onAllNodesWithTag("root").assertCountEquals(0)

    windowSize = tablet
    assertLabel("root", PaneRole.Primary, taps = 0)
    assertLabel("supporting1", PaneRole.DetailRoot, taps = 0)
  }

  @Test
  fun reservedPaneShowsPlaceholderUntilAPick() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    onNodeWithText("Pick one").assertExists()

    runOnIdle { navigator.goTo(Detail(1)) }
    assertLabel("detail1", PaneRole.DetailRoot, taps = 0)
    onAllNodesWithText("Pick one").assertCountEquals(0)

    runOnIdle { navigator.pop() }
    onNodeWithText("Pick one").assertExists()
  }

  @Test
  fun rootWithoutReservationFillsWideWindow() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    assertLabel("root", PaneRole.Primary, taps = 0)
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width)
  }

  @Test
  fun reservationSizesTheListPane() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    // There are no window insets in tests
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
  }

  @Test
  fun reservationAppliesOnTheFirstFrame() = runComposeUiTest {
    windowSize = tablet
    mainClock.autoAdvance = false
    setNavContent(ReservingRoot)
    mainClock.advanceTimeByFrame()
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
  }

  @Test
  fun listPaneCoversTheStartInset() = runComposeUiTest {
    windowSize = tablet
    windowInsets = WindowInsets(left = 40.dp)
    setNavContent(ReservingRoot)
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth + 40.dp)
  }

  @Test
  fun listPaneSkipsAStartInsetThatARailConsumed() = runComposeUiTest {
    windowSize = tablet
    windowInsets = WindowInsets(left = 40.dp)
    // A rail that pads itself for the inset
    leadingWidth = RailWidth + 40.dp
    consumedByChrome = WindowInsets(left = 40.dp)
    mainClock.autoAdvance = false
    setNavContent(ReservingRoot)
    // The first frame already has the width
    mainClock.advanceTimeByFrame()
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
    mainClock.autoAdvance = true
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
  }

  @Test
  fun supportingPaneSkipsAnEndInsetThatAParentConsumed() = runComposeUiTest {
    windowSize = tablet
    windowInsets = WindowInsets(right = 40.dp)
    setNavContent(Root)
    runOnIdle { navigator.goTo(Supporting(1)) }
    onNodeWithTag("supporting1-container").assertWidthIsEqualTo(tabletListPaneWidth + 40.dp)

    consumedByChrome = WindowInsets(right = 40.dp)
    onNodeWithTag("supporting1-container").assertWidthIsEqualTo(tabletListPaneWidth)
  }

  @Test
  fun startPaneKeepsTheEndInsetWhileItHasTheFullWidth() = runComposeUiTest {
    windowSize = tablet
    windowInsets = WindowInsets(right = 40.dp)
    // The end inset that each pane container sees as consumed, in px
    val consumedEnd = mutableMapOf<PanePosition, Int>()
    paneContainer = { position, content ->
      val density = LocalDensity.current
      Box(
        Modifier.onConsumedWindowInsetsChanged { consumed ->
          consumedEnd[position] = consumed.getRight(density, LayoutDirection.Ltr)
        }
      ) {
        content()
      }
    }
    setNavContent(Root)
    // Nothing is open beside the root, so it reaches the end edge
    runOnIdle { check(consumedEnd[PanePosition.Start] == 0) { "consumed: $consumedEnd" } }

    runOnIdle { navigator.goTo(Detail(1)) }
    runOnIdle { check(consumedEnd[PanePosition.Start] == 40) { "consumed: $consumedEnd" } }
  }

  private val testHandle: @Composable () -> Unit = {
    Box(Modifier.size(24.dp, 48.dp).testTag("handle"))
  }

  @Test
  fun dragHandleMovesTheEdgeUnderThePointer() = runComposeUiTest {
    windowSize = tablet
    dragHandle = testHandle
    setNavContent(ReservingRoot)
    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("detail1-tap").performClick()
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)

    // The first move passes the touch slop, so its distance is not exact
    onNodeWithTag("handle").performTouchInput {
      down(center)
      moveBy(Offset(60f, 0f))
    }
    val afterSlop = onNodeWithTag("root-container").getUnclippedBoundsInRoot().width
    check(afterSlop > tabletListPaneWidth) { "width: $afterSlop" }

    // The next move lands in one frame, with no animation
    mainClock.autoAdvance = false
    onNodeWithTag("handle").performTouchInput { moveBy(Offset(40f, 0f)) }
    mainClock.advanceTimeByFrame()
    onNodeWithTag("root-container").assertWidthIsEqualTo(afterSlop + 40.dp)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - afterSlop - 40.dp)
    mainClock.autoAdvance = true
    onNodeWithTag("handle").performTouchInput { up() }

    runOnIdle { check(host.draggedSplit == PaneSplit.Fixed(afterSlop + 40.dp)) }
    // The detail kept its state while it resized
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)

    // The split of the scene returns when the app clears the dragged one
    runOnIdle { host.draggedSplit = null }
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
  }

  @Test
  fun dragHandleStopsAtTheMinimumPaneWidth() = runComposeUiTest {
    windowSize = tablet
    dragHandle = testHandle
    setNavContent(ReservingRoot)
    runOnIdle { navigator.goTo(Detail(1)) }

    onNodeWithTag("handle").performTouchInput {
      down(center)
      moveBy(Offset(2000f, 0f))
    }
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(200.dp)
    onNodeWithTag("handle").performTouchInput {
      moveBy(Offset(-4000f, 0f))
      up()
    }
    onNodeWithTag("root-container").assertWidthIsEqualTo(200.dp)
  }

  @Test
  fun dragHandleResizesASupportingPane() = runComposeUiTest {
    windowSize = tablet
    dragHandle = testHandle
    setNavContent(Root)
    runOnIdle { navigator.goTo(Supporting(1)) }
    onNodeWithTag("supporting1-container").assertWidthIsEqualTo(tabletListPaneWidth)

    onNodeWithTag("handle").performTouchInput {
      down(center)
      moveBy(Offset(-100f, 0f))
      up()
    }
    // The edge moved toward the start, so the supporting pane grew
    val supportingWidth = onNodeWithTag("supporting1-container").getUnclippedBoundsInRoot().width
    check(supportingWidth > tabletListPaneWidth) { "width: $supportingWidth" }
    runOnIdle { check(host.draggedSplit == PaneSplit.EndFixed(supportingWidth)) }
  }

  @Test
  fun dragHandleFollowsTheLayoutDirection() = runComposeUiTest {
    windowSize = tablet
    layoutDirection = LayoutDirection.Rtl
    dragHandle = testHandle
    setNavContent(ReservingRoot)
    runOnIdle { navigator.goTo(Detail(1)) }

    // The start pane is on the right, so a move to the left makes it wider
    onNodeWithTag("handle").performTouchInput {
      down(center)
      moveBy(Offset(-100f, 0f))
      up()
    }
    val rootWidth = onNodeWithTag("root-container").getUnclippedBoundsInRoot().width
    check(rootWidth > tabletListPaneWidth) { "width: $rootWidth" }
  }

  @Test
  fun dragHandleHidesWhereTheEdgeCannotMove() = runComposeUiTest {
    windowSize = tablet
    dragHandle = testHandle
    hinge = DpRect(480.dp, 0.dp, 500.dp, 700.dp)
    setNavContent(Root)
    // One pane
    onNodeWithTag("handle").assertIsNotDisplayed()
    // A hinge fixes the edge
    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("root-container").assertWidthIsEqualTo(480.dp)
    onNodeWithTag("handle").assertIsNotDisplayed()
  }

  @Test
  fun dragHandleHidesWithThreePanes() = runComposeUiTest {
    windowSize = tablet
    maxPanes = 3
    dragHandle = testHandle
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("handle").assertIsDisplayed()
    runOnIdle { navigator.goTo(Extra(1)) }
    onNodeWithTag("handle").assertIsNotDisplayed()
  }

  @Test
  fun hingeSplitsThePanes() = runComposeUiTest {
    windowSize = tablet
    hinge = DpRect(480.dp, 0.dp, 500.dp, 700.dp)
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    onNodeWithTag("root-container").assertWidthIsEqualTo(480.dp)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(500.dp)
  }

  @Test
  fun replacedDetailKeepsItsRoleWhileLeaving() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    runOnIdle { navigator.goTo(Detail(1)) }
    assertLabel("detail1", PaneRole.DetailRoot, taps = 0)

    mainClock.autoAdvance = false
    runOnIdle { navigator.goTo(Detail(2)) }
    mainClock.advanceTimeByFrame()
    mainClock.advanceTimeByFrame()
    // Both are composed mid-transition, and the leaving one hasn't become a stacked screen
    onNodeWithTag("detail1").assertTextContains("role=DetailRoot", substring = true)
    onNodeWithTag("detail2").assertTextContains("role=DetailRoot", substring = true)
  }

  @Test
  fun hingeLinesUpWhenTheDecorationStartsPastTheWindowEdge() = runComposeUiTest {
    windowSize = tablet
    leadingWidth = 80.dp
    // In window coordinates, so 400dp from the start of the decoration
    hinge = DpRect(480.dp, 0.dp, 500.dp, 700.dp)
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    onNodeWithTag("root-container").assertWidthIsEqualTo(400.dp)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(500.dp)
  }

  @Test
  fun panelSlidesInWhileThePrimaryResizes() = runComposeUiTest {
    windowSize = tablet
    // No placeholder, so the root has the full width until a detail opens
    setNavContent(Root)
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width)

    mainClock.autoAdvance = false
    runOnIdle { navigator.goTo(Detail(1)) }
    repeat(4) { mainClock.advanceTimeByFrame() }
    // Part of the way there: the root is narrower, and has not reached its final width
    val midBounds = onNodeWithTag("root-container").getUnclippedBoundsInRoot()
    val midWidth = midBounds.right - midBounds.left
    check(midWidth < tablet.width) { "Expected the root to start shrinking, was $midWidth" }
    check(midWidth > tabletListPaneWidth) { "Expected an animation, was $midWidth" }
    // The detail keeps its final width while it slides in
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - tabletListPaneWidth)

    mainClock.autoAdvance = true
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)

    // A resize jumps to the new widths
    windowSize = DpSize(900.dp, 700.dp)
    mainClock.autoAdvance = false
    mainClock.advanceTimeByFrame()
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(900.dp - tabletListPaneWidth)
  }

  @Test
  fun foldAndUnfoldKeepState() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    onNodeWithTag("root-tap").performClick()
    onNodeWithTag("root-tap").performClick()
    assertLabel("root", PaneRole.Primary, taps = 2)

    windowSize = phone
    assertLabel("root", PaneRole.Single, taps = 2)

    windowSize = tablet
    assertLabel("root", PaneRole.Primary, taps = 2)

    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("detail1-tap").performClick()
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)

    windowSize = phone
    assertLabel("detail1", PaneRole.Single, taps = 1)
    onAllNodesWithTag("root").assertCountEquals(0)

    windowSize = tablet
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)
    assertLabel("root", PaneRole.Primary, taps = 2)
  }

  @Test
  fun pickingAgainReplacesTheDetail() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    runOnIdle { navigator.goTo(Detail(1)) }
    assertLabel("detail1", PaneRole.DetailRoot, taps = 0)
    runOnIdle { navigator.goTo(Detail(2)) }
    assertLabel("detail2", PaneRole.DetailRoot, taps = 0)
    onAllNodesWithTag("detail1").assertCountEquals(0)

    runOnIdle { navigator.pop() }
    onNodeWithText("Pick one").assertExists()
  }

  @Test
  fun hostReportsTheLayout() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    runOnIdle {
      // The root has the full width, so the window shows one pane of a scene
      check(host.scene != null)
      check(!host.isSplit)
      check(host.paneCount == 1)
      check(host.detailRoot == null)
      navigator.goTo(Detail(1))
    }
    runOnIdle {
      check(host.isSplit)
      check(host.paneCount == 2)
      check(host.detailRoot == Detail(1))
    }

    windowSize = phone
    runOnIdle {
      check(!host.isSplit)
      check(host.scene == null)
    }
  }

  @Test
  fun containerDrawsChromeInTheBoundsOfEachPane() = runComposeUiTest {
    // Chrome over each pane, like a floating toolbar
    paneContainer = { position, content ->
      Box {
        content()
        Box(Modifier.matchParentSize().testTag("chrome-$position"))
      }
    }
    windowSize = phone
    setNavContent(Root)
    onNodeWithTag("chrome-Single").assertWidthIsEqualTo(phone.width)
    onAllNodesWithTag("chrome-Start").assertCountEquals(0)

    windowSize = tablet
    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("chrome-Start").assertWidthIsEqualTo(tabletListPaneWidth)
    onNodeWithTag("chrome-End").assertWidthIsEqualTo(tablet.width - tabletListPaneWidth)
    onAllNodesWithTag("chrome-Single").assertCountEquals(0)
  }

  @Test
  fun containerOfTheDelegateKeepsItsStateAcrossALayoutSwitch() = runComposeUiTest {
    // Counts how many times the chrome of each position starts over
    val starts = mutableMapOf<PanePosition, Int>()
    var delegateContainers = 0
    paneContainer = { position, content ->
      if (position != PanePosition.Start) {
        remember { delegateContainers++ }
      }
      remember(position) { starts[position] = (starts[position] ?: 0) + 1 }
      content()
    }
    windowSize = phone
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("detail1-tap").performClick()

    windowSize = tablet
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)
    windowSize = phone
    assertLabel("detail1", PaneRole.Single, taps = 1)
    runOnIdle {
      // One container follows the delegate from the single layout to the end pane and back
      check(delegateContainers == 1) { "delegate containers: $delegateContainers" }
      check(starts[PanePosition.Start] == 1) { "starts: $starts" }
    }
  }

  @Test
  fun containerGetsTheNestedScrollOfItsOwnPane() = runComposeUiTest {
    // A toolbar that hides on scroll listens through a connection like this one
    val scrolled = mutableMapOf<PanePosition, Float>()
    paneContainer = { position, content ->
      val connection = remember {
        object : NestedScrollConnection {
          override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
          ): Offset {
            scrolled[position] = (scrolled[position] ?: 0f) + consumed.y
            return Offset.Zero
          }
        }
      }
      Box(Modifier.nestedScroll(connection)) { content() }
    }
    windowSize = tablet
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    onNodeWithTag("detail1-scroll").performTouchInput { swipeUp() }
    runOnIdle {
      check((scrolled[PanePosition.End] ?: 0f) < 0f) { "scrolled: $scrolled" }
      check((scrolled[PanePosition.Start] ?: 0f) == 0f) { "scrolled: $scrolled" }
    }
  }

  @Test
  fun hostCanTurnOffThePlaceholderOfTheStrategy() = runComposeUiTest {
    windowSize = tablet
    strategy = ListDetailPaneStrategy(placeholder = { BasicText("Pick one") })
    setNavContent(Root)
    onNodeWithText("Pick one").assertExists()
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)

    runOnIdle { host.placeholder = PaneHost.NoPlaceholder }
    onAllNodesWithText("Pick one").assertCountEquals(0)
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width)
  }

  @Test
  fun supportingPanelGetsItsFullWidth() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    runOnIdle { navigator.goTo(Supporting(1)) }

    onNodeWithTag("supporting1-container").assertWidthIsEqualTo(tabletListPaneWidth)
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width - tabletListPaneWidth)
  }

  @Test
  fun hingeSplitsThePanesInRightToLeftLayouts() = runComposeUiTest {
    windowSize = tablet
    layoutDirection = LayoutDirection.Rtl
    // The chrome is on the right, so the decoration covers 0dp to 920dp of the window
    leadingWidth = 80.dp
    hinge = DpRect(480.dp, 0.dp, 500.dp, 700.dp)
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    // The start pane starts at the right edge and ends at the hinge
    onNodeWithTag("root-container").assertWidthIsEqualTo(920.dp - 500.dp)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(480.dp)
  }

  @Test
  fun strategyPlaceholderGetsTheScreenOfTheStartPane() = runComposeUiTest {
    windowSize = tablet
    strategy = ListDetailPaneStrategy(placeholder = { primary -> BasicText("Pick for $primary") })
    setNavContent(Root)
    onNodeWithText("Pick for $Root").assertExists()
  }

  @Test
  fun hingeCorrectionAfterTheFirstFrameDoesNotAnimate() = runComposeUiTest {
    windowSize = tablet
    leadingWidth = 80.dp
    hinge = DpRect(480.dp, 0.dp, 500.dp, 700.dp)
    strategy = ListDetailPaneStrategy(placeholder = { BasicText("Pick one") })
    mainClock.autoAdvance = false
    setNavContent(Root)
    // One frame to learn the position in the window, and one to lay out with it
    repeat(3) { mainClock.advanceTimeByFrame() }
    onNodeWithTag("root-container").assertWidthIsEqualTo(400.dp)
  }

  @Test
  fun windowWithRoomForThreePanesShowsAnExtraScreenInTheThird() = runComposeUiTest {
    windowSize = tablet
    maxPanes = 3
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }
    // Two panes until an extra screen opens
    assertLabel("detail1", PaneRole.DetailRoot, taps = 0)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - tabletListPaneWidth)
    onNodeWithTag("detail1-tap").performClick()

    runOnIdle { navigator.goTo(Extra(1)) }
    assertLabel("root", PaneRole.Primary, taps = 0)
    // The detail keeps its role and its state when it moves to the middle pane
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)
    assertLabel("extra1", PaneRole.ExtraRoot, taps = 0)
    onNodeWithTag("root-container").assertWidthIsEqualTo(tabletListPaneWidth)
    onNodeWithTag("extra1-container").assertWidthIsEqualTo(tabletListPaneWidth)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - tabletListPaneWidth * 2)

    // Another screen of the same type replaces the one in the third pane
    runOnIdle { navigator.goTo(Extra(2)) }
    assertLabel("extra2", PaneRole.ExtraRoot, taps = 0)
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)
    onAllNodesWithTag("extra1").assertCountEquals(0)

    runOnIdle { navigator.pop() }
    assertLabel("detail1", PaneRole.DetailRoot, taps = 1)
    onAllNodesWithTag("extra2").assertCountEquals(0)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - tabletListPaneWidth)
  }

  @Test
  fun startPaneCanSizeItself() = runComposeUiTest {
    windowSize = tablet
    strategy = ListDetailPaneStrategy(listPaneSizesItself = true)
    setNavContent(SelfSizedRoot)
    // Alone, the start pane gets the full width
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width)

    runOnIdle { navigator.goTo(Detail(1)) }
    // The rail, plus the preferred pane width for the list
    val startPaneWidth = RailWidth + tabletListPaneWidth
    onNodeWithTag("rail").assertWidthIsEqualTo(RailWidth)
    onNodeWithTag("root-container").assertWidthIsEqualTo(startPaneWidth)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - startPaneWidth)
    assertLabel("root", PaneRole.Primary, taps = 0)

    runOnIdle { navigator.pop() }
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width)
  }

  @Test
  fun screensReadTheLayoutOfTheirOwnComposition() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    runOnIdle {
      // The host gets the layout after a composition, so a screen that read it there would
      // compose once with the single layout.
      check(rootPaneStates.isNotEmpty())
      check(rootPaneStates.all { it.scene != null })
      navigator.goTo(Detail(1))
    }
    runOnIdle { check(rootPaneStates.last().detailRoot == Detail(1)) }
  }

  /** A rail beside a list. Beside a detail, the list picks its width and the row wraps both. */
  @Composable
  private fun SelfSizedRootUi(modifier: Modifier) {
    val paneState = LocalPaneState.current
    val sizesItself = paneState.primarySizesItself
    val role = LocalPaneRole.current
    val active = LocalRecordLifecycle.current.isActive
    val rowSize =
      if (sizesItself) {
        Modifier.fillMaxHeight()
      } else {
        Modifier.fillMaxSize()
      }
    Row(modifier.then(rowSize).testTag("root-container")) {
      Box(Modifier.width(RailWidth).fillMaxHeight().testTag("rail"))
      val listSize =
        if (sizesItself) {
          Modifier.width(paneState.preferredPaneWidth)
        } else {
          Modifier.weight(1f)
        }
      Box(listSize.fillMaxHeight()) {
        BasicText("root role=$role active=$active taps=0", Modifier.testTag("root"))
      }
    }
  }

  @Composable
  private fun TestUi(screen: Screen, modifier: Modifier) {
    if (screen == SelfSizedRoot) {
      SelfSizedRootUi(modifier)
      return
    }
    val tag =
      when (screen) {
        Root,
        ReservingRoot -> "root"
        is Detail -> "detail${screen.id}"
        is Supporting -> "supporting${screen.id}"
        is Stacked -> "stacked${screen.id}"
        is Extra -> "extra${screen.id}"
        else -> error("Unexpected screen $screen")
      }
    if (tag == "root") {
      val paneState = LocalPaneState.current
      SideEffect { rootPaneStates += paneState }
    }
    var taps by rememberSaveable { mutableIntStateOf(0) }
    val role = LocalPaneRole.current
    val active = LocalRecordLifecycle.current.isActive
    Column(modifier.fillMaxSize().testTag("$tag-container")) {
      BasicText("$tag role=$role active=$active taps=$taps", Modifier.testTag(tag))
      Box(Modifier.testTag("$tag-tap").size(48.dp).clickable { taps++ }) { BasicText("Tap") }
      // Scrolls, for tests of nested scroll in a pane
      Box(Modifier.testTag("$tag-scroll").size(100.dp).verticalScroll(rememberScrollState())) {
        Box(Modifier.size(100.dp, 1000.dp))
      }
    }
  }
}

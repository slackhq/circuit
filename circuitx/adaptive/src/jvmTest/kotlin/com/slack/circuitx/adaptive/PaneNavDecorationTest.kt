// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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

  /** A root that reserves the right pane, like a list with a "pick one" message. */
  private data object ReservingRoot : Screen

  /** A root that draws a rail beside its list and picks its own width in the left pane. */
  private data object SelfSizedRoot : Screen

  private data class Detail(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.LIST_DETAIL
  }

  private data class Supporting(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.SUPPORTING
  }

  /** A screen opened from a detail, so it stacks in the right pane. */
  private data class Stacked(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.LIST_DETAIL
  }

  private data class TestState(val screen: Screen) : CircuitUiState

  private val phone = DpSize(411.dp, 900.dp)
  /** An iPad mini in portrait, which keeps the phone layout. */
  private val portraitTablet = DpSize(744.dp, 1133.dp)
  // The test window is 1024px wide at density 1, so wider sizes would clamp
  private val tablet = DpSize(1000.dp, 700.dp)
  /** The list pane at [tablet]: 40% of the width, limited to 400dp. */
  private val tabletListPaneWidth = 400.dp
  private val RailWidth = 72.dp

  private val circuit =
    Circuit.Builder()
      .addPresenterFactory { screen, _, _ -> presenterOf { TestState(screen) } }
      .addUiFactory { _, _ -> ui<TestState> { state, modifier -> TestUi(state.screen, modifier) } }
      .build()

  private var windowSize by mutableStateOf(phone)
  private var hinge: DpRect? = null
  private val host = PaneHost()
  private var strategy: PaneStrategy = ListDetailPaneStrategy()
  /** The [PaneState] of each composition of the root screen, oldest first. */
  private val rootPaneStates = mutableListOf<PaneState>()
  private lateinit var navigator: Navigator

  private fun ComposeUiTest.setNavContent(root: Screen) {
    if (root == ReservingRoot) {
      host.placeholder = { BasicText("Pick one") }
    }
    setContent {
      CircuitCompositionLocals(circuit) {
        // Apps wrap navigation in this, which adds a lookahead pass to every layout
        SharedElementTransitionLayout {
          Box(Modifier.size(windowSize)) {
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
                window = { size -> PaneWindow(size, listOfNotNull(hinge)) },
              )
            }
            NavigableCircuitContent(navigator, backStack, decoration = decoration)
          }
        }
      }
    }
  }

  private fun ComposeUiTest.assertLabel(tag: String, role: PaneRole, taps: Int) {
    onNodeWithTag(tag).assertTextEquals("$tag role=$role active=true taps=$taps")
  }

  @Test
  fun phoneShowsOnlyTheTopScreen() = runComposeUiTest {
    windowSize = phone
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    assertLabel("detail1", PaneRole.SINGLE, taps = 0)
    onAllNodesWithTag("root").assertCountEquals(0)
  }

  @Test
  fun wideWindowShowsRootBesideDetail() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    runOnIdle { navigator.goTo(Detail(1)) }

    assertLabel("root", PaneRole.PRIMARY, taps = 0)
    assertLabel("detail1", PaneRole.DETAIL_ROOT, taps = 0)

    runOnIdle { navigator.goTo(Stacked(1)) }
    assertLabel("stacked1", PaneRole.DETAIL_STACKED, taps = 0)
    onAllNodesWithTag("root").assertCountEquals(1)
  }

  @Test
  fun mediumWindowKeepsThePhoneLayoutUntilRotated() = runComposeUiTest {
    windowSize = portraitTablet
    setNavContent(ReservingRoot)
    assertLabel("root", PaneRole.SINGLE, taps = 0)
    onAllNodesWithText("Pick one").assertCountEquals(0)

    runOnIdle { navigator.goTo(Supporting(1)) }
    assertLabel("supporting1", PaneRole.SINGLE, taps = 0)
    onAllNodesWithTag("root").assertCountEquals(0)

    windowSize = tablet
    assertLabel("root", PaneRole.PRIMARY, taps = 0)
    assertLabel("supporting1", PaneRole.DETAIL_ROOT, taps = 0)
  }

  @Test
  fun reservedPaneShowsPlaceholderUntilAPick() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    onNodeWithText("Pick one").assertExists()

    runOnIdle { navigator.goTo(Detail(1)) }
    assertLabel("detail1", PaneRole.DETAIL_ROOT, taps = 0)
    onAllNodesWithText("Pick one").assertCountEquals(0)

    runOnIdle { navigator.pop() }
    onNodeWithText("Pick one").assertExists()
  }

  @Test
  fun rootWithoutReservationFillsWideWindow() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    assertLabel("root", PaneRole.PRIMARY, taps = 0)
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
    assertLabel("detail1", PaneRole.DETAIL_ROOT, taps = 0)

    mainClock.autoAdvance = false
    runOnIdle { navigator.goTo(Detail(2)) }
    mainClock.advanceTimeByFrame()
    mainClock.advanceTimeByFrame()
    // Both are composed mid-transition, and the leaving one hasn't become a stacked screen
    onNodeWithTag("detail1").assertTextContains("role=DETAIL_ROOT", substring = true)
    onNodeWithTag("detail2").assertTextContains("role=DETAIL_ROOT", substring = true)
  }

  @Test
  fun foldAndUnfoldKeepState() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    onNodeWithTag("root-tap").performClick()
    onNodeWithTag("root-tap").performClick()
    assertLabel("root", PaneRole.PRIMARY, taps = 2)

    windowSize = phone
    assertLabel("root", PaneRole.SINGLE, taps = 2)

    windowSize = tablet
    assertLabel("root", PaneRole.PRIMARY, taps = 2)

    runOnIdle { navigator.goTo(Detail(1)) }
    onNodeWithTag("detail1-tap").performClick()
    assertLabel("detail1", PaneRole.DETAIL_ROOT, taps = 1)

    windowSize = phone
    assertLabel("detail1", PaneRole.SINGLE, taps = 1)
    onAllNodesWithTag("root").assertCountEquals(0)

    windowSize = tablet
    assertLabel("detail1", PaneRole.DETAIL_ROOT, taps = 1)
    assertLabel("root", PaneRole.PRIMARY, taps = 2)
  }

  @Test
  fun pickingAgainReplacesTheDetail() = runComposeUiTest {
    windowSize = tablet
    setNavContent(ReservingRoot)
    runOnIdle { navigator.goTo(Detail(1)) }
    assertLabel("detail1", PaneRole.DETAIL_ROOT, taps = 0)
    runOnIdle { navigator.goTo(Detail(2)) }
    assertLabel("detail2", PaneRole.DETAIL_ROOT, taps = 0)
    onAllNodesWithTag("detail1").assertCountEquals(0)

    runOnIdle { navigator.pop() }
    onNodeWithText("Pick one").assertExists()
  }

  @Test
  fun hostReportsTheLayout() = runComposeUiTest {
    windowSize = tablet
    setNavContent(Root)
    runOnIdle {
      check(host.isSplit)
      check(host.detailRoot == null)
      navigator.goTo(Detail(1))
    }
    runOnIdle { check(host.detailRoot == Detail(1)) }

    windowSize = phone
    runOnIdle { check(!host.isSplit) }
  }

  @Test
  fun leftPaneCanSizeItself() = runComposeUiTest {
    windowSize = tablet
    strategy = ListDetailPaneStrategy(listPaneSizesItself = true)
    setNavContent(SelfSizedRoot)
    // Alone, the left pane gets the full width
    onNodeWithTag("root-container").assertWidthIsEqualTo(tablet.width)

    runOnIdle { navigator.goTo(Detail(1)) }
    // The rail, plus 40% of the width beside it
    val leftPaneWidth = RailWidth + (tablet.width - RailWidth) * 0.4f
    onNodeWithTag("rail").assertWidthIsEqualTo(RailWidth)
    onNodeWithTag("root-container").assertWidthIsEqualTo(leftPaneWidth)
    onNodeWithTag("detail1-container").assertWidthIsEqualTo(tablet.width - leftPaneWidth)
    assertLabel("root", PaneRole.PRIMARY, taps = 0)

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
      check(rootPaneStates.all { it.isSplit })
      navigator.goTo(Detail(1))
    }
    runOnIdle { check(rootPaneStates.last().detailRoot == Detail(1)) }
  }

  /** A rail beside a list. Beside a detail, the list picks its width and the row wraps both. */
  @Composable
  private fun SelfSizedRootUi(modifier: Modifier) {
    val sizesItself = LocalPaneState.current.primarySizesItself
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
          Modifier.listPaneWidth()
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
    }
  }
}

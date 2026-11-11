// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.NavStackList
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test

class ListDetailPaneStrategyTest {
  private data object Home : Screen

  private data object ListScreen : Screen

  private data object Plain : Screen

  private data class Detail(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.LIST_DETAIL
  }

  private data class Supporting(val id: Int) : PaneScreen {
    override val paneStyle: PaneStyle
      get() = PaneStyle.SUPPORTING
  }

  private data class Arg(override val screen: Screen) : NavArgument {
    override val key: String
      get() = screen.toString()
  }

  private val phone = DpSize(411.dp, 900.dp)
  private val portraitTablet = DpSize(744.dp, 1133.dp)
  private val foldOpen = DpSize(851.dp, 882.dp)
  private val desktop = DpSize(1200.dp, 800.dp)

  private val strategy = ListDetailPaneStrategy()

  /** The left pane width of a scene with a computed split. */
  private val PaneScene.fixedWidth: Dp
    get() = (split as PaneSplit.Fixed).primaryWidth

  /** A stack of [screens], top first. */
  private fun stack(vararg screens: Screen): NavStackList<Arg> {
    return checkNotNull(navStackListOf(screens.map(::Arg)))
  }

  private fun scene(size: DpSize, vararg screens: Screen, hinge: DpRect? = null): PaneScene? {
    return strategy.scene(PaneWindow(size, listOfNotNull(hinge)), stack(*screens))
  }

  @Test
  fun phoneAndMediumWindowsShowOneRecord() {
    assertThat(scene(phone, Detail(1), Home)).isNull()
    assertThat(scene(portraitTablet, Detail(1), Home)).isNull()
  }

  @Test
  fun wideWindowPutsTheRootBesideADetail() {
    val scene = scene(desktop, Detail(1), Home)
    assertThat(scene).isNotNull()
    assertThat(scene!!.primaryKey).isEqualTo(Arg(Home).key)
    assertThat(scene.fixedWidth).isEqualTo(400.dp)
  }

  @Test
  fun listPaneWidthScalesWithTheWindow() {
    // 851 * 0.4
    assertThat(scene(foldOpen, Detail(1), Home)!!.fixedWidth).isEqualTo(851.dp * 0.4f)
    assertThat(scene(DpSize(2000.dp, 900.dp), Detail(1), Home)!!.fixedWidth).isEqualTo(400.dp)
  }

  @Test
  fun listPaneCanSizeItself() {
    val selfSized = ListDetailPaneStrategy(listPaneSizesItself = true)
    fun split(vararg screens: Screen): PaneSplit {
      return selfSized.scene(PaneWindow(desktop), stack(*screens))!!.split
    }
    assertThat(split(Detail(1), Home)).isEqualTo(PaneSplit.PrimaryWraps())
    assertThat(split(Home)).isEqualTo(PaneSplit.PrimaryWraps())
    // The right pane is the narrow one for a supporting screen, so its width stays computed
    assertThat(split(Supporting(1), Home)).isInstanceOf<PaneSplit.Fixed>()
  }

  @Test
  fun supportingPaneLeavesMostOfTheWidthToThePrimary() {
    // The right pane takes 1200 * 0.33, which is within its limits.
    assertThat(scene(desktop, Supporting(1), Home)!!.fixedWidth)
      .isEqualTo(1200.dp - 1200.dp * 0.33f)
    // 851 * 0.33 is under the 320dp minimum.
    assertThat(scene(foldOpen, Supporting(1), Home)!!.fixedWidth).isEqualTo(851.dp - 320.dp)
  }

  @Test
  fun rootAloneKeepsThePanes() {
    val scene = scene(desktop, Home)
    assertThat(scene!!.primaryKey).isEqualTo(Arg(Home).key)
    assertThat(scene.fixedWidth).isEqualTo(400.dp)
  }

  @Test
  fun screenWithoutAStyleAboveTheRootShowsOneRecord() {
    assertThat(scene(desktop, Plain, Home)).isNull()
    assertThat(scene(desktop, Plain, Detail(1), Home)).isNull()
  }

  @Test
  fun screensAboveADetailStayInTheRightPane() {
    val scene = scene(desktop, Detail(2), Detail(1), Home)
    assertThat(scene!!.primaryKey).isEqualTo(Arg(Home).key)
  }

  @Test
  fun recordBelowTheTopmostRunTakesTheLeftPane() {
    val scene = scene(desktop, Detail(1), ListScreen, Home)
    assertThat(scene!!.primaryKey).isEqualTo(Arg(ListScreen).key)
  }

  @Test
  fun bottomOfTheRunPicksTheStyle() {
    val scene = scene(desktop, Detail(1), Supporting(1), Home)
    assertThat(scene!!.fixedWidth).isEqualTo(1200.dp - 1200.dp * 0.33f)
  }

  @Test
  fun rootWithAStyleStillTakesTheLeftPane() {
    val scene = scene(desktop, Detail(2), Detail(1))
    assertThat(scene!!.primaryKey).isEqualTo(Arg(Detail(1)).key)
  }

  @Test
  fun lookupReplacesTheMarker() {
    val lookup =
      ListDetailPaneStrategy(paneStyle = { if (it == Plain) PaneStyle.SUPPORTING else null })
    val window = PaneWindow(desktop)
    assertThat(lookup.scene(window, stack(Plain, Home))!!.primaryKey).isEqualTo(Arg(Home).key)
    assertThat(lookup.scene(window, stack(Detail(1), Home))).isNull()
  }

  @Test
  fun hingeWithRoomOnBothSidesSplitsThePanes() {
    val hinge = DpRect(420.dp, 0.dp, 431.dp, 882.dp)
    assertThat(scene(foldOpen, Detail(1), Home, hinge = hinge)!!.split).isEqualTo(PaneSplit.AtHinge)
  }

  @Test
  fun hingeTooCloseToAnEdgeIsIgnored() {
    val hinge = DpRect(200.dp, 0.dp, 211.dp, 882.dp)
    assertThat(scene(foldOpen, Detail(1), Home, hinge = hinge)!!.split)
      .isInstanceOf<PaneSplit.Fixed>()
  }

  @Test
  fun breakpointsAreConfigurable() {
    val narrow = ListDetailPaneStrategy(minWidthDp = 600, minHeightDp = 0)
    val window = PaneWindow(portraitTablet)
    assertThat(narrow.scene(window, stack(Detail(1), Home))).isNotNull()
  }
}

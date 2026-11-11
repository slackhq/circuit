// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuitx.adaptive

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isTrue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuitx.navigation.intercepting.InterceptingNavigator
import kotlin.test.Test

class PaneReplaceInterceptorTest {
  private data object Home : Screen

  private data object ListScreen : Screen

  private data class Detail(val id: Int) : Screen

  private data class Stacked(val id: Int) : Screen

  private val host = PaneHost()

  /**
   * A navigator over [screens], root first, with [primaryDepth] records in or below the left pane.
   */
  private fun navigator(
    vararg screens: Screen,
    primaryDepth: Int = 1,
    split: Boolean = true,
  ): Navigator {
    if (split) {
      host.scene = PaneScene(primaryKey = "primary", split = PaneSplit.Fixed(300.dp))
      host.primaryDepth = primaryDepth
    }
    val delegate = FakeNavigator(screens.first(), *screens.drop(1).toTypedArray())
    return InterceptingNavigator(delegate, listOf(PaneReplaceInterceptor(host)))
  }

  @Test
  fun singleLayoutAlwaysPushes() {
    val navigator = navigator(Home, Detail(1), split = false)
    assertThat(navigator.goTo(Detail(2))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(2), Detail(1), Home)
  }

  @Test
  fun newTypePushes() {
    val navigator = navigator(Home, Detail(1))
    assertThat(navigator.goTo(Stacked(1))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Stacked(1), Detail(1), Home)
  }

  @Test
  fun sameTypeReplaces() {
    val navigator = navigator(Home, Detail(1))
    assertThat(navigator.goTo(Detail(2))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(2), Home)
  }

  @Test
  fun sameTypeReplacesEverythingAboveIt() {
    val navigator = navigator(Home, Detail(1), Stacked(1))
    assertThat(navigator.goTo(Detail(2))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(2), Home)
  }

  @Test
  fun openScreenIsNotPushedAgain() {
    val navigator = navigator(Home, Detail(1))
    assertThat(navigator.goTo(Detail(1))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(1), Home)
  }

  @Test
  fun openScreenBelowOthersPopsBackToIt() {
    val navigator = navigator(Home, Detail(1), Stacked(1))
    assertThat(navigator.goTo(Detail(1))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(1), Home)
  }

  @Test
  fun primaryRecordIsNeverReplaced() {
    val navigator = navigator(Detail(1))
    assertThat(navigator.goTo(Detail(2))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(2), Detail(1))
  }

  @Test
  fun recordsBelowThePrimaryAreNeverReplaced() {
    // Detail(1) is hidden below the list in the left pane.
    val navigator = navigator(Home, Detail(1), ListScreen, primaryDepth = 3)
    assertThat(navigator.goTo(Detail(2))).isTrue()
    assertThat(navigator.peekBackStack()).containsExactly(Detail(2), ListScreen, Detail(1), Home)
  }
}

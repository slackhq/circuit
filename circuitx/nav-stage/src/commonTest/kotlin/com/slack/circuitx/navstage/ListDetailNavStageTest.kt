// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalNavStageApi::class)

package com.slack.circuitx.navstage

import com.slack.circuit.runtime.navigation.NavArgument
import com.slack.circuit.runtime.navigation.navStackListOf
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertEquals

class ListDetailNavStageTest {
  private val stage = ListDetailNavStage<Arg>(isListPane = { it is ListScreen })

  @Test
  fun visibleItemsPairsNearestListWithActive() {
    val list = Arg(ListScreen)
    val detail = Arg(DetailScreen)
    val args = navStackListOf(activeItem = detail, backwardItems = listOf(list, Arg(OtherScreen)))

    assertEquals(listOf(list, detail), stage.visibleItems(args))
  }

  @Test
  fun visibleItemsFallsBackToActiveWithoutList() {
    val detail = Arg(DetailScreen)
    val args = navStackListOf(activeItem = detail, backwardItems = listOf(Arg(OtherScreen)))

    assertEquals(listOf(detail), stage.visibleItems(args))
  }
}

private data class Arg(override val screen: Screen) : NavArgument {
  override val key: String = screen.toString()
}

private data object ListScreen : Screen

private data object DetailScreen : Screen

private data object OtherScreen : Screen

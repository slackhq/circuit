// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.Screen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize

private val HOME_NAV_ITEMS = persistentListOf(BottomNavItem.Adoptables, BottomNavItem.About)

@Parcelize
object HomeNavScreen : Screen {
  data class State(
    val index: Int,
    val bottomNavItems: ImmutableList<BottomNavItem>,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class ClickNavItem(val index: Int) : Event
  }
}

@Composable
fun HomeNavPresenter(): HomeNavScreen.State {
  var index by remember { mutableStateOf(0) }
  return HomeNavScreen.State(index = index, bottomNavItems = HOME_NAV_ITEMS) { event ->
    when (event) {
      is HomeNavScreen.Event.ClickNavItem -> index = event.index
    }
  }
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
class HomeNavState {
  val navItems: ImmutableList<BottomNavItem> =
    persistentListOf(BottomNavItem.Adoptables, BottomNavItem.About)
  var index by mutableStateOf(0)
}

// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import com.slack.circuit.Screen
import com.slack.circuit.star.petlist.PetListScreen

private const val DOGS_SCREEN_NAME = "Adoptables"
private const val ABOUT_SCREEN_NAME = "About"

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
  abstract val screen: Screen
  object Adoptables : BottomNavItem(DOGS_SCREEN_NAME, Icons.Filled.Home) {
    override val screen: Screen = PetListScreen
  }
  object About : BottomNavItem(ABOUT_SCREEN_NAME, Icons.Filled.Info) {
    override val screen: Screen = AboutScreen
  }
}

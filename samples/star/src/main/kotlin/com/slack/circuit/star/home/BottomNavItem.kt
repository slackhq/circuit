/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

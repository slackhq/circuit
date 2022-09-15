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
package com.slack.circuit.sample.home

import com.slack.circuit.Screen
import com.slack.circuit.sample.petlist.AboutScreen
import com.slack.circuit.sample.petlist.PetListScreen

private const val DOGS_SCREEN_NAME = "Dogs"
private const val ABOUT_SCREEN_NAME = "About"
const val DOGS_SCREEN_INDEX = 0
const val ABOUT_SCREEN_INDEX = 1

sealed class BottomNavItem(val title: String, val screen: Screen, val index: Int) {
  object Dogs : BottomNavItem(DOGS_SCREEN_NAME, PetListScreen, DOGS_SCREEN_INDEX)
  object About : BottomNavItem(ABOUT_SCREEN_NAME, AboutScreen, ABOUT_SCREEN_INDEX)
}

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
package com.slack.circuit.star.navigator

import android.content.Intent
import com.slack.circuit.Navigator
import com.slack.circuit.Screen
import kotlinx.parcelize.Parcelize

/** Custom navigator that adds support for initiating navigation in standard Android. */
class AndroidSupportingNavigator(
  private val navigator: Navigator,
  private val onAndroidScreen: (AndroidScreen) -> Unit
) : Navigator by navigator {
  override fun goTo(screen: Screen) =
    when (screen) {
      is AndroidScreen -> onAndroidScreen(screen)
      else -> navigator.goTo(screen)
    }
}

sealed interface AndroidScreen : Screen {
  @Parcelize data class CustomTabsIntentScreen(val url: String) : AndroidScreen
  @Parcelize data class IntentScreen(val intent: Intent) : AndroidScreen
}

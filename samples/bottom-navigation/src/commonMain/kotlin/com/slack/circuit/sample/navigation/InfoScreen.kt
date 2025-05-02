// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.ui.platform.UriHandler
import com.slack.circuit.internal.runtime.IgnoreOnParcel
import com.slack.circuit.internal.runtime.Parcelize
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.InterceptedGoToResult

@Parcelize
object InfoScreen : Screen {
  @IgnoreOnParcel const val url = "https://slackhq.github.io/circuit/"
}

class InfoScreenInterceptor(private val uriHandler: UriHandler) : NavigationInterceptor {
  override fun goTo(screen: Screen): InterceptedGoToResult {
    return when (screen) {
      is InfoScreen -> {
        uriHandler.openUri(screen.url)
        NavigationInterceptor.ConsumedSuccess
      }
      else -> NavigationInterceptor.Skipped
    }
  }
}

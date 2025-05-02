// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalSharedTransitionApi::class)
fun main() {

  val tabs = TabScreen.all
  val circuit = buildCircuitForTabs(tabs)
  application {
    Window(title = "Navigation Sample", onCloseRequest = ::exitApplication) {
      MaterialTheme {
        val backStack = rememberSaveableBackStack(tabs.first())
        val navigator = rememberCircuitNavigator(backStack) { exitApplication() }
        // CircuitX Navigation
        val uriHandler = LocalUriHandler.current
        val interceptors = remember { persistentListOf(InfoScreenInterceptor(uriHandler)) }
        val interceptingNavigator =
          rememberInterceptingNavigator(navigator = navigator, interceptors = interceptors)
        CircuitCompositionLocals(circuit) {
          SharedElementTransitionLayout {
            ContentScaffold(backStack, interceptingNavigator, tabs, Modifier.fillMaxSize())
          }
        }
      }
    }
  }
}

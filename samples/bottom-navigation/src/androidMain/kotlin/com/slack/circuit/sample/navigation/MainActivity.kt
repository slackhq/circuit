// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.slack.circuit.backstack.rememberSaveableNavStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.navigation.intercepting.AndroidScreenAwareNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.InterceptedGoToResult
import com.slack.circuitx.navigation.intercepting.LogcatLogger
import com.slack.circuitx.navigation.intercepting.LoggingNavigationEventListener
import com.slack.circuitx.navigation.intercepting.LoggingNavigatorFailureNotifier
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Set edge to edge + dark status bar icons
    enableEdgeToEdge()
    window
      ?.let { WindowCompat.getInsetsController(it, window.decorView) }
      ?.isAppearanceLightStatusBars = true

    // CircuitX Navigation
    val interceptors =
      listOf(AndroidScreenAwareNavigationInterceptor(this), InfoScreenRewriteInterceptor)
    val eventListeners = listOf(LoggingNavigationEventListener(LogcatLogger))
    val notifier = LoggingNavigatorFailureNotifier(LogcatLogger)

    val tabs = TabScreen.all
    val circuit = buildCircuitForTabs(tabs)
    setContent {
      MaterialTheme {
        val navStack = rememberSaveableNavStack(ContentScreen(tabs))
        val navigator = rememberCircuitNavigator(navStack)
        // Build the delegate Navigator.
        val interceptingNavigator =
          rememberInterceptingNavigator(
            navigator = navigator,
            interceptors = interceptors,
            eventListeners = eventListeners,
            notifier = notifier,
          )
        CircuitCompositionLocals(circuit) {
          NavigableCircuitContent(
            navigator = interceptingNavigator,
            navStack = navStack,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

private object InfoScreenRewriteInterceptor : NavigationInterceptor {
  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedGoToResult {
    return when (screen) {
      is InfoScreen -> {
        InterceptedGoToResult.Rewrite(
          IntentScreen(Intent(Intent.ACTION_VIEW, "https://slackhq.github.io/circuit/".toUri()))
        )
      }
      else -> NavigationInterceptor.Skipped
    }
  }
}

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.slack.circuitx.navigation.intercepting.AndroidScreenAwareNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.CircuitNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.LoggingNavigationEventListener
import com.slack.circuitx.navigation.intercepting.LoggingNavigatorFailureNotifier
import com.slack.circuitx.navigation.intercepting.rememberCircuitInterceptingNavigator
import kotlinx.collections.immutable.persistentListOf

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
      persistentListOf(AndroidScreenAwareNavigationInterceptor(this), InfoScreenRewriteInterceptor)
    val eventListeners = persistentListOf(LoggingNavigationEventListener)
    val notifier = LoggingNavigatorFailureNotifier

    val tabs = TabScreen.all
    setContent {
      MaterialTheme {
        val backStack = rememberSaveableBackStack(tabs.first())
        val navigator = rememberCircuitNavigator(backStack)
        // Build the delegate Navigator.
        val interceptingNavigator =
          rememberCircuitInterceptingNavigator(
            navigator = navigator,
            interceptors = interceptors,
            eventListeners = eventListeners,
            notifier = notifier,
          )
        val circuit =
          remember(navigator) {
            buildCircuitForTabs(tabs)
              .newBuilder()
              .setAnimatedNavDecoratorFactory(
                GestureNavigationDecorationFactory(onBackInvoked = { interceptingNavigator.pop() })
              )
              .build()
          }
        CircuitCompositionLocals(circuit) {
          ContentScaffold(backStack, interceptingNavigator, tabs, Modifier.fillMaxSize())
        }
      }
    }
  }
}

private object InfoScreenRewriteInterceptor : CircuitNavigationInterceptor {
  override fun goTo(screen: Screen): CircuitNavigationInterceptor.Result {
    return when (screen) {
      is InfoScreen -> {
        CircuitNavigationInterceptor.Result.Rewrite(
          NavEvent.GoTo(
            IntentScreen(Intent(Intent.ACTION_VIEW, "https://slackhq.github.io/circuit/".toUri()))
          )
        )
      }
      else -> CircuitNavigationInterceptor.Skipped
    }
  }
}

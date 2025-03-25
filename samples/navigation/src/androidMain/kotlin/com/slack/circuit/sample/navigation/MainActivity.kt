// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.navigation

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuitx.android.IntentScreen
import com.slack.circuitx.navigation.intercepting.AndroidScreenAwareNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.CircuitInterceptingNavigator
import com.slack.circuitx.navigation.intercepting.CircuitNavigationInterceptor
import com.slack.circuitx.navigation.intercepting.LoggingNavigationEventListener
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
      persistentListOf(
        AndroidScreenAwareNavigationInterceptor { screen ->
          when (screen) {
            is IntentScreen -> screen.startWith(this)
            else -> error("Unknown AndroidScreen: $screen")
          }
        }
      )
    val eventListeners = persistentListOf(LoggingNavigationEventListener)
    val notifier =
      object : CircuitInterceptingNavigator.FailureNotifier {
        override fun goToInterceptorFailure(result: CircuitNavigationInterceptor.Result.Failure) {
          Log.w("Circuit", "goToInterceptorFailure: $result")
        }

        override fun popInterceptorFailure(result: CircuitNavigationInterceptor.Result.Failure) {
          Log.w("Circuit", "popInterceptorFailure: $result")
        }
      }

    val tabs = TabScreen.all
    val circuit = buildCircuitForTabs(tabs)
    setContent {
      MaterialTheme {
        val backStack = rememberSaveableBackStack(tabs.first())
        val navigator = rememberCircuitNavigator(backStack)
        // Build the delegate Navigator.
        val interceptingNavigator =
          rememberCircuitInterceptingNavigator(
            backStack = backStack,
            navigator = navigator,
            interceptors = interceptors,
            eventListeners = eventListeners,
            notifier = notifier,
          )
        CircuitCompositionLocals(circuit) {
          ContentScaffold(backStack, interceptingNavigator, tabs, Modifier.fillMaxSize())
        }
      }
    }
  }
}

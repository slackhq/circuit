// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.SingletonImageLoader
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.navigation.NavigationContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuit.star.di.AppComponent
import com.slack.circuit.star.home.HomeScreen
import com.slack.circuit.star.navigation.OpenUrlScreen
import com.slack.circuit.star.ui.StarTheme
import java.awt.Desktop
import java.net.URI
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalSharedTransitionApi::class)
fun main() {
  val component = AppComponent.create()
  SingletonImageLoader.setSafe { component.imageLoader }
  application {
    val initialBackStack = persistentListOf<Screen>(HomeScreen)
    val backStack = rememberSaveableBackStack(initialBackStack)
    val circuitNavigator = rememberCircuitNavigator(backStack) { exitApplication() }
    val navigator =
      remember(circuitNavigator) {
        object : Navigator by circuitNavigator {
          override fun goTo(screen: Screen, context: NavigationContext): Boolean {
            return when (screen) {
              is OpenUrlScreen -> openUrl(screen.url)
              else -> circuitNavigator.goTo(screen, context)
            }
          }
        }
      }
    val windowState =
      rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(Alignment.Center),
      )
    var darkMode by remember { mutableStateOf(false) }
    Window(
      title = "STAR",
      state = windowState,
      onCloseRequest = ::exitApplication,
      // In lieu of a global shortcut handler, we best-effort with this
      // https://github.com/JetBrains/compose-multiplatform/issues/914
      onKeyEvent = { event ->
        when {
          // Cmd+W
          event.key == Key.W && event.isMetaPressed && event.type == KeyEventType.KeyDown -> {
            exitApplication()
            true
          }
          // Cmd+U
          // Toggles dark mode
          event.key == Key.U && event.isMetaPressed && event.type == KeyEventType.KeyDown -> {
            darkMode = !darkMode
            true
          }
          // Backpress ish
          event.key == Key.Escape -> {
            if (backStack.size > 1) {
              navigator.pop()
              true
            } else {
              false
            }
          }
          else -> false
        }
      },
    ) {
      StarTheme(useDarkTheme = darkMode) {
        CircuitCompositionLocals(component.circuit) {
          SharedElementTransitionLayout {
            ContentWithOverlays {
              NavigableCircuitContent(navigator = navigator, backStack = backStack)
            }
          }
        }
      }
    }
  }
}

private fun openUrl(url: String): Boolean {
  val desktop = Desktop.getDesktop()
  desktop.browse(URI.create(url))
  return true
}

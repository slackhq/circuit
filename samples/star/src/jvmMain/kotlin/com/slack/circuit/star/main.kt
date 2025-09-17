// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.SingletonImageLoader
import com.slack.circuit.star.di.AppGraph
import java.awt.Desktop
import java.net.URI

fun main() {
  val appGraph = AppGraph.create()
  SingletonImageLoader.setSafe { appGraph.imageLoader }
  application {
    val windowState =
      rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(Alignment.Center),
      )
    var darkMode by remember { mutableStateOf(false) }
    val uriHandler = remember { DesktopUriHandler() }
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
      val state = rememberStarAppState(useDarkTheme = darkMode)
      Window(
        title = "STAR",
        state = windowState,
        onCloseRequest = ::exitApplication,
        alwaysOnTop = true,
        // In lieu of a global shortcut handler, we best-effort with this
        // https://youtrack.jetbrains.com/issue/CMP-5337
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
              if (state.backStack.size > 1) {
                state.navigator.pop()
                true
              } else {
                false
              }
            }
            else -> false
          }
        },
      ) {
        StarCircuitApp(circuit = appGraph.circuit, state = state)
      }
    }
  }
}

class DesktopUriHandler() : UriHandler {
  override fun openUri(uri: String) {
    val desktop = Desktop.getDesktop()
    desktop.browse(URI.create(uri))
  }
}

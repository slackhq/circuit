// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import coil3.SingletonImageLoader
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.star.di.AppComponent
import com.slack.circuit.star.home.HomeScreen
import kotlinx.collections.immutable.persistentListOf

fun main() {
  val component = AppComponent.create()
  SingletonImageLoader.setSafe { component.imageLoader }
  application {
    Window(
      title = "STAR",
      state = WindowState(width = 300.dp, height = 600.dp),
      onCloseRequest = ::exitApplication,
    ) {
      val initialBackStack = persistentListOf<Screen>(HomeScreen)
      val backStack = rememberSaveableBackStack { initialBackStack.forEach(::push) }
      val navigator = rememberCircuitNavigator(backStack, ::exitApplication)

      MaterialTheme {
        CircuitCompositionLocals(component.circuit) {
          ContentWithOverlays {
            NavigableCircuitContent(
              navigator = navigator,
              backstack = backStack,
              modifier =
                Modifier.backHandler(
                  enabled = backStack.size > 1,
                  onBack = { navigator.pop() },
                )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Modifier.backHandler(enabled: Boolean, onBack: () -> Unit): Modifier {
  val focusRequester = remember { FocusRequester() }
  return focusRequester(focusRequester)
    .onPlaced { focusRequester.requestFocus() }
    .onPreviewKeyEvent {
      if (enabled && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
        onBack()
        true
      } else {
        false
      }
    }
}

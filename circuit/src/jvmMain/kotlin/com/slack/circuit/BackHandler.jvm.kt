// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.slack.circuit.backstack.BackStack

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun BackHandler(
  navigator: Navigator,
  backstack: BackStack<*>,
  content: @Composable () -> Unit,
) {
  if (backstack.size > 1) {
    val focusRequester = remember { FocusRequester() }
    val modifier =
      Modifier.onPreviewKeyEvent {
          if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
            navigator.pop()
            true
          } else false
        }
        .focusRequester(focusRequester)
        .focusable()

    Box(modifier) { content() }

    SideEffect { focusRequester.requestFocus() }
  } else {
    content()
  }
}

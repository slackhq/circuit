// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.BackStack

@Composable
internal actual fun BackHandler(
  navigator: Navigator,
  backstack: BackStack<*>,
  content: @Composable () -> Unit,
) {
  BackHandler(
    enabled = backstack.size > 1,
    onBack = navigator::pop,
  )
  content()
}

// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit

import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.BackStack

@Composable
internal expect fun BackHandler(
  navigator: Navigator,
  backstack: BackStack<*>,
  content: @Composable () -> Unit,
)

// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.compose.ui.window.ComposeUIViewController
import com.slack.circuit.retained.CircuitRetainedSettings
import com.slack.circuit.retained.ExperimentalCircuitRetainedApi
import com.slack.circuit.retained.RetainedValuesStoreProvider
import com.slack.circuit.star.di.AppGraph
import platform.UIKit.UIViewController

@OptIn(ExperimentalCircuitRetainedApi::class)
fun makeUiViewController(graph: AppGraph): UIViewController {
  CircuitRetainedSettings.useFirstParty = true
  return ComposeUIViewController {
    RetainedValuesStoreProvider { StarCircuitApp(graph.circuit) }
  }
}

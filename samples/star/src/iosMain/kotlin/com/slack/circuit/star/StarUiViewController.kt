// Copyright (C) 2024 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star

import androidx.compose.ui.window.ComposeUIViewController
import com.slack.circuit.star.di.AppGraph
import platform.UIKit.UIViewController

fun makeUiViewController(graph: AppGraph): UIViewController = ComposeUIViewController {
  StarCircuitApp(graph.circuit)
}

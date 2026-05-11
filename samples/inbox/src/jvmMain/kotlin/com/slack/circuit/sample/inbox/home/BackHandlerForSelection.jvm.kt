// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.sample.inbox.home

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerForSelection(active: Boolean, onBack: () -> Unit) {
  // Desktop has no system back gesture; the user clears the selection via the in-pane back button.
}

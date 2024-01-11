// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun BackPressNavIcon(
  modifier: Modifier,
  onClick: (() -> Unit)?,
  iconButtonContent: @Composable () -> Unit,
) {
  // We do nothing on desktop
  // TODO maybe we do escaping?
}

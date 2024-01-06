// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun BackPressNavIcon(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  iconButtonContent: @Composable () -> Unit,
)

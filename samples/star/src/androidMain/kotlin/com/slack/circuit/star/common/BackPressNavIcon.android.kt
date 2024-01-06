// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
actual fun BackPressNavIcon(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    iconButtonContent: @Composable () -> Unit,
) {
  val backPressOwner = LocalOnBackPressedDispatcherOwner.current
  val finalOnClick = remember {
    onClick
        ?: backPressOwner?.onBackPressedDispatcher?.let { dispatcher -> dispatcher::onBackPressed }
        ?: error("No local LocalOnBackPressedDispatcherOwner found.")
  }
  IconButton(modifier = modifier, onClick = finalOnClick) { iconButtonContent() }
}

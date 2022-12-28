// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun BackPressNavIcon(
  modifier: Modifier = Modifier,
  iconButtonContent: @Composable () -> Unit = { ClosedIconImage() },
) {
  val onBackPressedDispatcher =
    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      ?: error("No local LocalOnBackPressedDispatcherOwner found.")
  IconButton(modifier = modifier, onClick = onBackPressedDispatcher::onBackPressed) {
    iconButtonContent()
  }
}

@Composable
private fun ClosedIconImage(modifier: Modifier = Modifier) {
  Image(
    modifier = modifier,
    painter = rememberVectorPainter(image = Icons.Filled.Close),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
    contentDescription = "Close",
  )
}

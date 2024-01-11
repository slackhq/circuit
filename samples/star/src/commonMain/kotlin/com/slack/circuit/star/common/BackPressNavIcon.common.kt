// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
expect fun BackPressNavIcon(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  iconButtonContent: @Composable () -> Unit = { ClosedIconImage() },
)

@Composable
internal fun ClosedIconImage(modifier: Modifier = Modifier) {
  Image(
    modifier = modifier,
    painter = rememberVectorPainter(image = Icons.Filled.Close),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
    contentDescription = "Close",
  )
}

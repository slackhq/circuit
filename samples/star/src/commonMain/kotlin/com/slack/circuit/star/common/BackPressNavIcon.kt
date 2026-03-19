// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.common

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
fun BackPressNavIcon(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  iconButtonContent: @Composable () -> Unit = { ClosedIconImage() },
) {
  val finalOnClick = onClick ?: rememberDispatchBackClick() ?: return
  IconButton(modifier = modifier, onClick = finalOnClick) { iconButtonContent() }
}

/** Returns a click handler that dispatches a back event through the navigation event system. */
@Composable
private fun rememberDispatchBackClick(): (() -> Unit)? {
  val dispatcher =
    LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return null
  val input = remember(dispatcher) {
    DirectNavigationEventInput().also { dispatcher.addInput(it) }
  }
  DisposableEffect(input) { onDispose { dispatcher.removeInput(input) } }
  return remember(input) {
    {
      input.backStarted(NavigationEvent())
      input.backCompleted()
    }
  }
}

@Composable
internal fun ClosedIconImage(modifier: Modifier = Modifier) {
  Image(
    modifier = modifier,
    painter = rememberVectorPainter(image = Icons.Filled.Close),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
    contentDescription = "Close",
  )
}

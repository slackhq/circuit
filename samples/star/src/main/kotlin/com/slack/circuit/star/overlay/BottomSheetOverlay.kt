// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package com.slack.circuit.star.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.star.ui.rememberStableCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val onDismiss: () -> Result,
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {
  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    val sheetState = rememberModalBottomSheetState()

    val coroutineScope = rememberStableCoroutineScope()
    BackHandler(enabled = sheetState.isVisible) {
      coroutineScope
        .launch { sheetState.hide() }
        .invokeOnCompletion {
          if (!sheetState.isVisible) {
            navigator.finish(onDismiss())
          }
        }
    }

    ModalBottomSheet(
      modifier = Modifier.fillMaxWidth(),
      content = {
        // Delay setting the result until we've finished dismissing
        content(model) { result ->
          // This is the OverlayNavigator.finish() callback
          coroutineScope.launch {
            try {
              sheetState.hide()
            } finally {
              navigator.finish(result)
            }
          }
        }
      },
      sheetState = sheetState,
      shape = RoundedCornerShape(32.dp),
      onDismissRequest = { navigator.finish(onDismiss()) },
    )

    LaunchedEffect(Unit) { sheetState.show() }
  }
}

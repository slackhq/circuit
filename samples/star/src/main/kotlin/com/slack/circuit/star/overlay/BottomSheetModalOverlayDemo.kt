/*
 * Copyright (C) 2022 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slack.circuit.star.overlay

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.slack.circuit.LocalOverlayHost
import com.slack.circuit.Overlay
import com.slack.circuit.OverlayHost
import com.slack.circuit.OverlayNavigator
import kotlinx.coroutines.launch

interface ModalResult {
  object Ok : ModalResult
  object Cancel : ModalResult
  object Dismiss : ModalResult
}

@ExperimentalMaterialApi
class BottomSheet<T : Any>(
  private val model: String,
  private val dismissOnTapOutside: Boolean = true,
  private val onDismiss: (() -> T)? = null,
  private val content: @Composable (String, OverlayNavigator<T>) -> Unit,
) : Overlay<T> {
  @Composable
  override fun Content(navigator: OverlayNavigator<T>) {
    val sheetState =
      rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { dismissOnTapOutside }
      )
    var pendingResult by remember { mutableStateOf<T?>(null) }
    var hasShown by remember { mutableStateOf(false) }
    if (hasShown && sheetState.currentValue == ModalBottomSheetValue.Hidden) {
      val result = pendingResult ?: onDismiss?.invoke() ?: error("no result!")
      navigator.finish(result)
    }
    ModalBottomSheetLayout(
      modifier = Modifier.fillMaxSize(),
      sheetContent = {
        // If this all looks dumb, it's because it is.
        // https://github.com/google/accompanist/issues/910
        Box(Modifier.padding(32.dp)) {
          Box(Modifier.fillMaxSize(0.51f))
          // Delay setting the result until we've finished dismissing
          val coroutineScope = rememberCoroutineScope()
          content(model) { result ->
            pendingResult = result
            coroutineScope.launch { sheetState.hide() }
          }
        }
      },
      sheetState = sheetState,
      sheetShape = RoundedCornerShape(32.dp)
    ) {
      // Nothing here, left to the existing content
    }
    LaunchedEffect(sheetState) {
      sheetState.show()
      hasShown = true
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
suspend fun OverlayHost.showDetailsSheet(model: String): ModalResult {
  return show(
    BottomSheet<ModalResult>(
      model = model,
      onDismiss = { ModalResult.Dismiss },
    ) { bottomSheetModel, dialogNavigator ->
      Column {
        Text(
          text = bottomSheetModel,
        )

        Button(onClick = { dialogNavigator.finish(ModalResult.Ok) }) { Text("Ok") }

        Button(onClick = { dialogNavigator.finish(ModalResult.Cancel) }) { Text("Cancel") }
      }
    }
  )
}

data class ModalMessageModel(val title: String, val detailMessage: String?)

@Composable
fun ModalMessage(
  model: ModalMessageModel,
) {
  val scope = rememberCoroutineScope()
  val overlayHost = LocalOverlayHost.current
  val context = LocalContext.current

  Button(
    enabled = model.detailMessage != null,
    onClick = {
      scope.launch {
        val result = overlayHost.showDetailsSheet(model.detailMessage!!)
        Toast.makeText(context, "Result: ${result.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
      }
    }
  ) { Text("Open modal") }
}

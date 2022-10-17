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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.Overlay
import com.slack.circuit.OverlayNavigator
import kotlinx.coroutines.launch

class BottomSheetOverlay<Model : Any, Result : Any>(
  private val model: Model,
  private val dismissOnTapOutside: Boolean = true,
  private val onFinish: (() -> Unit)? = null,
  private val onDismiss: (() -> Result)? = null,
  private val content: @Composable (Model, OverlayNavigator<Result>) -> Unit,
) : Overlay<Result> {
  @Composable
  override fun Content(navigator: OverlayNavigator<Result>) {
    val sheetState =
      rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = {
          if (dismissOnTapOutside) {
            if (it == ModalBottomSheetValue.Hidden) {
              // This is apparently as close as we can get to an "onDismiss" callback, which
              // unfortunately has no animation
              val result = onDismiss?.invoke() ?: error("no result!")
              navigator.finish(result)
            }
            onFinish?.invoke()
            true
          } else {
            false
          }
        }
      )

    val coroutineScope = rememberCoroutineScope()
    val localNavigator = remember(onFinish, sheetState, navigator) {
      object : OverlayNavigator<Result> by navigator {
        override fun finish(result: Result?) {
          // Delay setting the result until we've finished dismissing
          coroutineScope.launch {
            sheetState.hide()
            navigator.finish(result)
            onFinish?.invoke()
          }
        }
      }
    }

    ModalBottomSheetLayout(
      modifier = Modifier.fillMaxSize(),
      sheetContent = {
        // If this all looks dumb, it's because it is.
        // https://github.com/google/accompanist/issues/910
        Box(Modifier.padding(32.dp)) {
          Box(Modifier.fillMaxSize(0.51f))
          content(model, localNavigator)
        }
      },
      sheetState = sheetState,
      sheetShape = RoundedCornerShape(32.dp)
    ) {
      // Nothing here, left to the existing content
    }
    LaunchedEffect(sheetState) { sheetState.show() }
  }
}

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
package com.slack.circuit.star.common

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    contentDescription = "Close",
  )
}
